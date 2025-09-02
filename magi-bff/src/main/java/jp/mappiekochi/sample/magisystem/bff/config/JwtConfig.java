package jp.mappiekochi.sample.magisystem.bff.config;

import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfig {

    private static final String TENANT = "58b3dae2-fc4f-431f-a033-090ce281f42c";
    private static final String EXPECTED_AUD_API = "api://bc98cd63-de49-4c9d-9664-fc9d14f8ce60";
    private static final String EXPECTED_AUD_CLIENTID = "bc98cd63-de49-4c9d-9664-fc9d14f8ce60";

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
            JwtDecoders.fromOidcIssuerLocation("https://login.microsoftonline.com/" + TENANT + "/v2.0");

        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String iss = token.getIssuer() != null ? token.getIssuer().toString() : "";
            String url = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
            if (url.equals(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Invalid issuer",""));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            Object aud = token.getClaims().get("aud");
            if (aud instanceof String) {
                if (EXPECTED_AUD_API.equals(aud) || EXPECTED_AUD_CLIENTID.equals(aud)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } else if (aud instanceof Collection) {
                if (((Collection<?>)aud).contains(EXPECTED_AUD_API) || ((Collection<?>)aud).contains(EXPECTED_AUD_CLIENTID)) {
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
