package it.pagopa.pn.delivery.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.pagopa.pn.delivery.models.AsseverationEvent;

import java.io.IOException;

public class AsseverationMoreFieldSerializer extends StdSerializer<AsseverationEvent.Payload.AsseverationMoreField> {


    protected AsseverationMoreFieldSerializer(Class<AsseverationEvent.Payload.AsseverationMoreField> t) {
        super(t);
    }

    @Override
    public void serialize(AsseverationEvent.Payload.AsseverationMoreField value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeEndObject();
    }
}
