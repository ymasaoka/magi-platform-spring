package jp.mappiekochi.sample.magisystem.bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CoreClientService {

    private static final Logger logger = LoggerFactory.getLogger(CoreClientService.class);

    private final WebClient coreWebClient;
    private final DaprClient daprClient;
    private final TokenExchangeService tokenExchangeService;
    private final String coreScope;
    private final boolean useDapr;
    private final String daprAppId;

    public CoreClientService(WebClient coreWebClient,
                             DaprClient daprClient,
                             TokenExchangeService tokenExchangeService,
                             @Value("${magi.core.scope}") String coreScope,
                             @Value("${magi.core.use-dapr:false}") boolean useDapr,
                             @Value("${magi.core.dapr-app-id:magi-core}") String daprAppId) {
        this.coreWebClient = coreWebClient;
        this.daprClient = daprClient;
        this.tokenExchangeService = tokenExchangeService;
        this.coreScope = coreScope;
        this.useDapr = useDapr;
        this.daprAppId = daprAppId;
    }

    public Mono<ResponseEntity<String>> getCoreResource(String path, Jwt jwt) {
        return tokenExchangeService.exchangeOnBehalfOf(jwt, coreScope)
                .flatMap(coreToken -> {
                    logger.debug("Forwarding GET to core: path='{}', Authorization='Bearer {}'", path, truncate(coreToken));
                    if (useDapr) {
                        InvokeMethodRequest req = new InvokeMethodRequest(daprAppId, "api/magi/" + path)
                                .setHttpExtension(HttpExtension.GET);
                        req.getMetadata().put("Authorization", "Bearer " + coreToken);

                        return daprClient
                                .invokeMethod(req, new TypeRef<String>() {})
                                .map(resp -> ResponseEntity.ok(resp));
                    }
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
                    if (useDapr) {
                        return daprClient
                                .invokeMethod(daprAppId, "api/" + path, body == null ? "" : body, HttpExtension.POST, String.class)
                                .map(resp -> ResponseEntity.ok(resp));
                    }
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
