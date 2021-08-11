package it.pagopa.pn.commons.configs;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConditionalOnProperty( name = "pn.middleware.init.cassandra", havingValue = "true")
@Slf4j
public class PnCassandraAutoConfiguration extends CassandraAutoConfiguration {

    @Autowired
    private RuntimeMode runtimeMode;

    @Bean
    @ConditionalOnMissingBean
    @Scope("prototype")
    public CqlSessionBuilder cassandraSessionBuilder(CassandraProperties properties,
                                                     DriverConfigLoader driverConfigLoader, ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers) {

        if( RuntimeMode.DEVELOPMENT.equals( runtimeMode ) ) {
            log.info("Custom cassandra autoconfigurator");

            String keyspaceName = properties.getKeyspaceName();
            if(StringUtils.isNotBlank( keyspaceName )) {
                properties.setKeyspaceName( null );
                super.cassandraSessionBuilder( properties, driverConfigLoader, builderCustomizers)
                        .build()
                        .execute("CREATE KEYSPACE IF NOT EXISTS " + keyspaceName + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}");
                properties.setKeyspaceName( keyspaceName );
            }
        }

        return super.cassandraSessionBuilder( properties, driverConfigLoader, builderCustomizers);
    }


}
