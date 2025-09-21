package jp.mappiekochi.sample.magisystem.bff.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.HttpExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/core")
public class CoreProxyController {

    private static final Logger logger = LoggerFactory.getLogger(CoreProxyController.class);

    private final WebClient webClient;
    private final String coreBase;
    private final DaprClient daprClient;
    private final boolean useDapr;
    private final String daprAppId;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public CoreProxyController(WebClient coreWebClient,
            @Value("${magi.core.base-url}") String coreBase,
            DaprClient daprClient,
            @Value("${magi.core.use-dapr:false}") boolean useDapr,
            @Value("${magi.core.dapr-app-id:magi-core}") String daprAppId) {
        this.webClient = coreWebClient;
        this.coreBase = coreBase;
        this.daprClient = daprClient;
        this.useDapr = useDapr;
        this.daprAppId = daprAppId;
    }

    @PostMapping("/magi/vote")
    public Mono<ResponseEntity<String>> vote(@RequestBody String body,
            @RequestHeader HttpHeaders incomingHeaders) {
        logger.debug("Forwarding POST /api/magi/vote to core: {}", coreBase + "/vote");

        if (useDapr) {
            // Dapr 経由（非同期）
            // target path must match magi-core's endpoint
            InvokeMethodRequest req = new InvokeMethodRequest(daprAppId, "api/magi/vote")
                    .setHttpExtension(HttpExtension.POST);

            Object parsedBody;
            if (body == null || body.isBlank()) {
                parsedBody = new HashMap<String, Object>();
            } else {
                try {
                    parsedBody = OBJECT_MAPPER.readValue(body,
                            new TypeReference<java.util.Map<String, Object>>() {
                            });
                } catch (Exception e) {
                    logger.warn("Failed to parse request body as JSON; sending raw string as fallback: {}",
                            e.toString());
                    parsedBody = body;
                }
            }
            req.setBody(parsedBody);

            Map<String, String> metadata = req.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
                req.setMetadata(metadata);
            }
            metadata.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            List<String> auth = incomingHeaders.get(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isEmpty()) {
                metadata.put("Authorization", auth.get(0));
            }

            return daprClient
                    .invokeMethod(req, new io.dapr.utils.TypeRef<byte[]>() {
                    })
                    .map(respBytes -> {
                        String resp = respBytes == null ? "" : new String(respBytes, StandardCharsets.UTF_8);
                        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(resp == null ? "" : resp);
                    })
                    .onErrorResume(ex -> {
                        logger.error("Error forwarding to core via Dapr: {}", ex.toString(), ex);
                        return Mono
                                .just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to contact core"));
                    });
        }

        // 既存の WebClient 経路
        return webClient.post()
                .uri("/vote")
                .headers(h -> {
                    List<String> auth = incomingHeaders.get(HttpHeaders.AUTHORIZATION);
                    if (auth != null && !auth.isEmpty()) {
                        h.set(HttpHeaders.AUTHORIZATION, auth.get(0));
                    }
                    List<String> reqIds = incomingHeaders.get("X-Request-Id");
                    if (reqIds != null && !reqIds.isEmpty()) {
                        h.set("X-Request-Id", reqIds.get(0));
                    }
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("Client error from core")
                        .flatMap(msg -> Mono.error(new ResponseStatusException(resp.statusCode(), msg))))
                .onStatus(status -> status.is5xxServerError(), resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("Server error from core")
                        .flatMap(msg -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg))))
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(60))
                .map(entity -> {
                    HttpHeaders responseHeaders = new HttpHeaders();
                    entity.getHeaders().forEach((k, v) -> responseHeaders.put(k, v));
                    return ResponseEntity.status(entity.getStatusCode()).headers(responseHeaders)
                            .body(entity.getBody());
                })
                .onErrorResume(throwable -> {
                    logger.error("Error forwarding to core: {}", throwable.toString());
                    if (throwable instanceof ResponseStatusException) {
                        ResponseStatusException rse = (ResponseStatusException) throwable;
                        return Mono.just(ResponseEntity.status(rse.getStatusCode()).body(rse.getReason()));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to contact core"));
                });
    }
}
