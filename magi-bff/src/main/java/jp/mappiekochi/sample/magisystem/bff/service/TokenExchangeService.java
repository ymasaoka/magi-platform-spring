package jp.mappiekochi.sample.magisystem.bff.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
public class TokenExchangeService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;

    public TokenExchangeService(WebClient.Builder webClientBuilder,
                                @Value("${azure.client.id}") String clientId,
                                @Value("${azure.client.secret}") String clientSecret,
                                @Value("${azure.token-uri}") String tokenUri) {
        this.webClient = webClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
    }

    /**
     * Exchange incoming user JWT for an access token to call downstream (on-behalf-of).
     * Returns a Mono that emits the access_token string.
     */
    public Mono<String> exchangeOnBehalfOf(Jwt incomingJwt, String scope) {
        if (incomingJwt == null || incomingJwt.getTokenValue() == null) {
            return Mono.error(new IllegalArgumentException("incoming JWT is required for OBO exchange"));
        }

        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .with("requested_token_use", "on_behalf_of")
                        .with("assertion", incomingJwt.getTokenValue())
                        .with("scope", scope))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::accessToken);
    }

    // minimal immutable DTO for Azure token response -> record で簡潔に
    static record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("scope") String scope
    ) {}
}
