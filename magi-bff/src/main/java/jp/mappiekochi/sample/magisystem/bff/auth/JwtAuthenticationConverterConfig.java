package jp.mappiekochi.sample.magisystem.bff.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.stream.Collectors;

@Configuration
public class JwtAuthenticationConverterConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object scpClaim = jwt.getClaims().get("scp");
            Collection<String> scopes;
            if (scpClaim instanceof Collection<?>) {
                scopes = ((Collection<?>) scpClaim).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
            } else {
                scopes = java.util.Collections.emptyList();
            }
            return scopes.stream()
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .collect(Collectors.toList());
        });
        return conv;
    }
}
