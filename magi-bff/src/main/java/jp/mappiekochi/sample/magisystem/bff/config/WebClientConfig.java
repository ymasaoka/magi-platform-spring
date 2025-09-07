package jp.mappiekochi.sample.magisystem.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String coreBaseUrl;

    // public WebClientConfig(@Value("${magi.core.base-url}") String coreBaseUrl) {
    public WebClientConfig(@Value("${bff.core.base-url}") String coreBaseUrl) {
        this.coreBaseUrl = coreBaseUrl;
    }

    @Bean
    public WebClient coreWebClient(WebClient.Builder builder) {
        return builder.baseUrl(coreBaseUrl).build();
    }
}
