package it.pagopa.pn.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsGlobalConfiguration implements WebFluxConfigurer {

    @Value("${cors.allowed.domains:}")
    private List<String> corsAllowedDomains;

    @PostConstruct
    private void init() {
        if( corsAllowedDomains == null || corsAllowedDomains.isEmpty() ) {
            corsAllowedDomains = Arrays.asList("http://localhost:8080", "http://localhost:8090");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedOrigins( corsAllowedDomains.toArray( new String[0] ) )
                .allowedMethods("GET", "HEAD", "OPTIONS", "POST", "PUT", "DELETE", "PATCH")
                .maxAge(3600);
    }
}