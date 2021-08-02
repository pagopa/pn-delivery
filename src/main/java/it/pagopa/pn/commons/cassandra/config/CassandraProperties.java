package it.pagopa.pn.commons.cassandra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.data.cassandra")
public class CassandraProperties {

    private String keyspaceName;
    private String username;
    private String password;
    private String entitypackage;

}
