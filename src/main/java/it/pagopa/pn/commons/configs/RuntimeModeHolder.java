package it.pagopa.pn.commons.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuntimeModeHolder {

    public static final String DEVELOPMENT_MODE_PROPERTY = "pn.env.runtime";

    @Value( "${" + DEVELOPMENT_MODE_PROPERTY + "}")
    private String runtimeModeName;

    @Bean
    public RuntimeMode runtimeMode() {
        return Enum.valueOf( RuntimeMode.class, runtimeModeName );
    }

}
