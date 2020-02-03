package main.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.NewsPost;
import main.tools.JsoupProxy;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
public class ContentHandlerService {


    private final ExecutorService executor = Executors.newWorkStealingPool(10);
    private AtomicInteger processedNewsCount = new AtomicInteger();

    private static final String DAY_SELECTOR = "a.ui-state-default";
    protected static final String HOST_NAME = "https://www.pravda.com.ua/";
    private static final String HREF_ATTRIBUTE_NAME = "href";

    private static final Logger LOG = LoggerFactory.
            getLogger(ContentHandlerService.class);

    @Autowired
    private SqsService sqsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Async
    public void run() {
        IntStream stream = IntStream.range(0, 20);
        stream.forEach(this::handleYearNewsPage);
    }

    private void handleYearNewsPage(int i) {
        try {
            final String year = (i < 10) ? ("0" + i) : String.valueOf(i);
            Document doc = JsoupProxy.connect(String.format("%1$sarchives/year_20%2$s", HOST_NAME, year));
            Elements days = doc.select(DAY_SELECTOR);
            days.forEach(this::handleDailyNewsPage);
        } catch (IOException | InterruptedException e) {
           LOG.error("Error occurred while trying to open a connection.", e);
        }
    }


    private void handleNewsPage(Element el) {
        String url = el.attr(HREF_ATTRIBUTE_NAME);
        if (url.startsWith("/news")) {
            url = HOST_NAME + url;
        }

        try {
            Document doc = JsoupProxy.connect(url);
            NewsPostSelector newsPostSelector = NewsPostSelector.get(doc.baseUri());

            Elements headerElement = doc.select(newsPostSelector.getHeader());
            Elements dateTimeElement = doc.select(newsPostSelector.getDateTime());
            Elements bodyElement = doc.select(newsPostSelector.getBody());

            NewsPost newsPost = new NewsPost(UUID.randomUUID().toString(),
                    dateTimeElement.text(), headerElement.text(), bodyElement.text());

            try {
                sqsService.sendMessage(objectMapper.writeValueAsString(newsPost));
            }catch (Exception e){
                LOG.error("Error occurred while trying to send an sqs message.", e);
            }

            LOG.info("Total processed news count: {}", processedNewsCount.incrementAndGet());

        } catch (IOException | InterruptedException e) {
            LOG.error("Error occurred while trying to open a connection.", e);
        }

    }


    private void handleDailyNewsPage(Element el) {
        executor.submit(() -> {
            final String url = el.attr(HREF_ATTRIBUTE_NAME);
            try {
                Document docNewsPost = JsoupProxy.connect(HOST_NAME + url);
                Elements news = docNewsPost.select("div.article__title a:not([href^=\"/columns\"])");
                news.forEach(this::handleNewsPage);

            } catch (IOException | InterruptedException e) {
                LOG.error("Error occurred while trying to open a connection.", e);
            }

        });
    }


}


class NewsPostSelector{
    private String header;
    private String dateTime;
    private String body;

    private static final Map<String, NewsPostSelector> NPS = new HashMap<>();

    private NewsPostSelector(String header, String dateTime, String body){
        this.header = header;
        this.dateTime = dateTime;
        this.body = body;
    }


    private static NewsPostSelector getNPS(String baseUri){
        return (!baseUri.startsWith(ContentHandlerService.HOST_NAME)) ? new NewsPostSelector("h1.post__title",
                "div.post__time", "div.post__text") :
                new NewsPostSelector("h1.post_news__title",
                        "div.post_news__date", "div.post_news__text");
    }

    public String getHeader() {
        return header;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getBody() {
        return body;
    }

    public static NewsPostSelector get(String baseUri){
        NewsPostSelector newsPostSelector = NPS.get(baseUri);
        if(newsPostSelector == null){
            newsPostSelector = getNPS(baseUri);
            NPS.put(baseUri, newsPostSelector);
        }
        return newsPostSelector;
    }
}
