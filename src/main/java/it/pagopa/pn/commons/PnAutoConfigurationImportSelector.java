package it.pagopa.pn.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

public class PnAutoConfigurationImportSelector extends AutoConfigurationImportSelector {

    @Override
    protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        Set<String> exclusions = super.getExclusions(metadata, attributes);

        String keyValueStore = getEnvironment().getProperty("pn.key-value-store");

        if( ! "cassandra".equals( keyValueStore )) {
            exclusions.add("org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration");
        }
        return exclusions;
    }

}
