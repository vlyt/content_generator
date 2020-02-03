package main.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class JsoupProxy {

    private static final Logger LOG = LoggerFactory.getLogger(JsoupProxy.class);

    private static final JsoupClient jsoupClient = new JsoupClient();
    private static final int DEFAULT_WAIT_TIMEOUT = 3000;
    private static final int CONNECTIONS_BEFORE_TIMEOUT = 15;
    private static final AtomicInteger connectionsCount = new AtomicInteger(1);

    public static Document connect(String url) throws IOException, InterruptedException {


            if (connectionsCount.get() % CONNECTIONS_BEFORE_TIMEOUT == 0) {

                synchronized (jsoupClient) {
                    jsoupClient.wait(DEFAULT_WAIT_TIMEOUT);
                }

            } else {
                connectionsCount.incrementAndGet();
            }

            Document doc = jsoupClient.connect(url);

            return doc;
    }

}
class JsoupClient {
    public Document connect(String url) throws IOException {
        return Jsoup.connect(url).get();
    }
}