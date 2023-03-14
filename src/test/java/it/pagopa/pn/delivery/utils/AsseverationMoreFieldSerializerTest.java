package it.pagopa.pn.delivery.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;


class AsseverationMoreFieldSerializerTest {

    private AsseverationMoreFieldSerializer asseverationMoreFieldSerializer;

    @BeforeEach
    void setup() {
        this.asseverationMoreFieldSerializer = new AsseverationMoreFieldSerializer();
    }

    @Test
    void serialize() throws IOException {

        Writer jsonWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        AsseverationEvent.Payload.AsseverationMoreField asseverationMoreField = AsseverationEvent.Payload.AsseverationMoreField.builder().build();
        asseverationMoreFieldSerializer.serialize(asseverationMoreField, jsonGenerator, serializerProvider);
        jsonGenerator.flush();
        Assertions.assertEquals( "{}" ,jsonWriter.toString() );
    }
}
