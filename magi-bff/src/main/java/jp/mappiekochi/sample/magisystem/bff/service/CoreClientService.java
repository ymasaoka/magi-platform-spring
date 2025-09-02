package jp.mappiekochi.sample.magisystem.bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CoreClientService {

    private static final Logger logger = LoggerFactory.getLogger(CoreClientService.class);

    private final WebClient coreWebClient;
    private final TokenExchangeService tokenExchangeService;
    private final String coreScope;

    public CoreClientService(WebClient coreWebClient,
                             TokenExchangeService tokenExchangeService,
                             @Value("${magi.core.scope}") String coreScope) {
        this.coreWebClient = coreWebClient;
        this.tokenExchangeService = tokenExchangeService;
        this.coreScope = coreScope;
    }

    public Mono<ResponseEntity<String>> getCoreResource(String path, Jwt jwt) {
        return tokenExchangeService.exchangeOnBehalfOf(jwt, coreScope)
                .flatMap(coreToken -> {
                    logger.debug("Forwarding GET to core: path='{}', Authorization='Bearer {}'", path, truncate(coreToken));
                    return coreWebClient.get()
                            .uri("/api/{path}", path)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + coreToken)
                            .retrieve()
                            .toEntity(String.class);
                });
    }

    public Mono<ResponseEntity<String>> postCoreResource(String path, String body, Jwt jwt) {
        return tokenExchangeService.exchangeOnBehalfOf(jwt, coreScope)
                .flatMap(coreToken -> {
                    logger.debug("Forwarding POST to core: path='{}', Authorization='Bearer {}'", path, truncate(coreToken));
                    logger.debug("Forwarding POST body to core ({}): {}", path, body);
                    return coreWebClient.post()
                            .uri("/api/{path}", path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + coreToken)
                            .bodyValue(body == null ? "" : body)
                            .retrieve()
                            .toEntity(String.class);
                });
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 8 ? s.substring(0, 8) + "..." : s;
    }
}
