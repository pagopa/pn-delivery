package it.pagopa.pn.commons.configs;

import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.*;

public class PnAutoConfigurationSelector extends AutoConfigurationImportSelector {

    private static final String MIDDLEWARE_DEACTIVATION_PREFIX = "pn.middleware.init.";

    private static final Map<String, List<String>> EXCLUSIONS_MAP = Map.ofEntries(
            Map.entry( MIDDLEWARE_DEACTIVATION_PREFIX + "cassandra",
                Arrays.asList(
                    CassandraAutoConfiguration.class.getName(),
                    PnCassandraAutoConfiguration.class.getName()
                )),
            Map.entry( MIDDLEWARE_DEACTIVATION_PREFIX + "kafka",
                Collections.singletonList(
                    KafkaAutoConfiguration.class.getName()
                ))
        );

    private static final List<String> DEVELOPMENT_EXCLUSION = Collections.singletonList(
            CassandraAutoConfiguration.class.getName()
        );

    private static final List<String> NOT_DEVELOPMENT_EXCLUSION = Collections.singletonList(
            PnCassandraAutoConfiguration.class.getName()
       );
    public static final String DEVELOPMENT_MODE_PROPERTY = "pn.env.development";


    private final Environment env;

    public PnAutoConfigurationSelector(Environment env) {
        this.env = env;
    }

    @Override
    protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        Set<String> exclusions = super.getExclusions(metadata, attributes);

        exclusions.addAll( computeMiddlewareExclusions() );

        if( isDevelopmentMode() ) {
            exclusions.addAll( DEVELOPMENT_EXCLUSION );
        }
        else {
            exclusions.addAll( NOT_DEVELOPMENT_EXCLUSION );
        }

        return exclusions;
    }

    private Set<String> computeMiddlewareExclusions() {
        Set<String> exclusions = new HashSet<>();

        for( Map.Entry<String, List<String>> entry: EXCLUSIONS_MAP.entrySet() ) {
            String propertyValue = env.getProperty( entry.getKey() );
            if( isFalse( propertyValue ) ) {
                exclusions.addAll( entry.getValue() );
            }
        }
        return exclusions;
    }

    private boolean isDevelopmentMode() {
        return "true".equalsIgnoreCase( env.getProperty( DEVELOPMENT_MODE_PROPERTY, "").trim() );
    }

    private boolean isFalse(String propertyValue) {
        return propertyValue != null && "false".equalsIgnoreCase( propertyValue.trim() );
    }

}
