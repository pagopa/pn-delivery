package it.pagopa.pn.commons.cassandra.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraConfig {

    private final String keyspace;
    private final String username;
    private final String password;

    CassandraConfig(
            @Value("${spring.data.cassandra.keyspace-name}") String keyspace,
            @Value("${spring.data.cassandra.contact-points}") String username,
            @Value("${spring.data.cassandra.contact-points}") String password) {
        this.keyspace = keyspace;
        this.username = username;
        this.password = password;
    }

    @Bean
    public CqlSession getSession(){
        return CqlSession.builder().withKeyspace(keyspace).withAuthCredentials(username,password).build();
    }
}
