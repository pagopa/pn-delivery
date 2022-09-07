package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

class ModelMapperFactoryTest {

    private ModelMapperFactory factory;

    @BeforeEach
    void setup() {
        this.factory = new ModelMapperFactory();
    }

    @Test
    void createModelMapper() {
        // When
        ModelMapper mapper = factory.createModelMapper( NewNotificationRequest.class , InternalNotification.class );

        // Then
        Assertions.assertNotNull( mapper );
        Assertions.assertNotNull( mapper.getTypeMap( NewNotificationRequest.class , InternalNotification.class ) );
        Assertions.assertEquals( MatchingStrategies.STRICT, mapper.getConfiguration().getMatchingStrategy() );
    }

}
