package jp.mappiekochi.sample.magisystem.bff.config;

import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class JwtConfig {

    @Value("${azure.client.tenant-id}")
    private String tenant;

    @Value("${azure.client.id}")
    private String azureClientId;

    @Value("${azure.client.aud.scope}")
    private String expectedAudApi;

    @Value("${azure.client.aud.id}")
    private String expectedAudClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
            JwtDecoders.fromOidcIssuerLocation("https://login.microsoftonline.com/" + tenant + "/v2.0");

        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String iss = token.getIssuer() != null ? token.getIssuer().toString() : "";
            String url = "https://login.microsoftonline.com/" + tenant + "/v2.0";
            if (url.equals(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Invalid issuer",""));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            Object aud = token.getClaims().get("aud");
            if (aud instanceof String) {
                if (expectedAudApi.equals(aud) || expectedAudClientId.equals(aud)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } else if (aud instanceof Collection) {
                if (((Collection<?>)aud).contains(expectedAudApi) || ((Collection<?>)aud).contains(expectedAudClientId)) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Invalid audience",""));
        };

        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(), issuerValidator, audienceValidator);
        jwtDecoder.setJwtValidator(combined);
        return jwtDecoder;
    }
}
