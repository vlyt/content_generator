package main.conf;



import main.services.ContentHandlerService;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {
    @Bean
    public ContentHandlerService webContentService() {
        return new ContentHandlerService();
    }

}
