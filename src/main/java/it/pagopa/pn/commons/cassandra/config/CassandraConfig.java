package it.pagopa.pn.commons.cassandra.config;


import it.pagopa.pn.delivery.PnDeliveryApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

    private final CassandraProperties cassandraProperties;

    @Autowired
    CassandraConfig(
            CassandraProperties cassandraProperties) {

        this.cassandraProperties = cassandraProperties;
    }

    @Override
    protected String getKeyspaceName() {
        return cassandraProperties.getKeyspaceName();
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {

        CreateKeyspaceSpecification specification = CreateKeyspaceSpecification
                .createKeyspace(cassandraProperties.getKeyspaceName())
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true);

        return Arrays.asList(specification);
    }

    @Bean
    @Override
    public CqlSessionFactoryBean cassandraSession() {
        CqlSessionFactoryBean cassandraSession = super.cassandraSession();//super session should be called only once
        cassandraSession.setUsername(cassandraProperties.getUsername());
        cassandraSession.setPassword(cassandraProperties.getPassword());
        return cassandraSession;
    }

    @Override // da indagare
    public String[] getEntityBasePackages() {
        return new String[] {cassandraProperties.getEntitypackage()};
    }

}


