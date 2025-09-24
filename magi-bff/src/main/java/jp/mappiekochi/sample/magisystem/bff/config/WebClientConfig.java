package jp.mappiekochi.sample.magisystem.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

@Configuration
public class WebClientConfig {

    private final String _coreBaseUrl;

    public WebClientConfig(@Value("${magi.core.base-url}") String coreBaseUrl) {
        this._coreBaseUrl = coreBaseUrl;
    }

    @Bean
    public WebClient coreWebClient(WebClient.Builder builder) {
        return builder.baseUrl(this._coreBaseUrl).build();
    }

    @Bean
    public DaprClient daprClient() {
        return new DaprClientBuilder().build();
    }
}
