package jp.mappiekochi.sample.magisystem.bff.controller;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/core")
public class CoreProxyController {

    private static final Logger logger = LoggerFactory.getLogger(CoreProxyController.class);

    private final WebClient webClient;
    private final String coreBase;

    public CoreProxyController(WebClient coreWebClient,
                               @Value("${magi.core.base-url}") String coreBase) {
        this.webClient = coreWebClient;
        this.coreBase = coreBase;
    }

    @PostMapping("/magi/vote")
    public ResponseEntity<String> vote(@RequestBody String body,
                                       @RequestHeader HttpHeaders incomingHeaders) {
        // ログに最低限の情報のみ出す（本番では機密漏洩に注意）
        logger.debug("Forwarding POST /api/magi/vote to core: {}", coreBase + "/vote");

        // 転送するヘッダーを作る（必要なものだけコピー）
        return webClient.post()
                .uri("/vote")
                .headers(h -> {
                    // Authorization をそのまま渡す
                    List<String> auth = incomingHeaders.get(HttpHeaders.AUTHORIZATION);
                    if (auth != null && !auth.isEmpty()) {
                        h.set(HttpHeaders.AUTHORIZATION, auth.get(0));
                    }
                    // 透過で渡したいカスタムヘッダ（例: X-Request-Id）
                    List<String> reqIds = incomingHeaders.get("X-Request-Id");
                    if (reqIds != null && !reqIds.isEmpty()) {
                        h.set("X-Request-Id", reqIds.get(0));
                    }
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(body)
                // タイムアウト/エラー処理：短めにしておく
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
                    // 可能なら元のステータス・ヘッダをそのまま返す
                    HttpHeaders responseHeaders = new HttpHeaders();
                    entity.getHeaders().forEach((k, v) -> responseHeaders.put(k, v));
                    return ResponseEntity.status(entity.getStatusCode()).headers(responseHeaders).body(entity.getBody());
                })
                .onErrorResume(throwable -> {
                    logger.error("Error forwarding to core: {}", throwable.toString());
                    // タイムアウトや接続失敗は 502 にマップ
                    if (throwable instanceof ResponseStatusException) {
                        ResponseStatusException rse = (ResponseStatusException) throwable;
                        return Mono.just(ResponseEntity.status(rse.getStatusCode()).body(rse.getReason()));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to contact core"));
                })
                .block();
    }
}
