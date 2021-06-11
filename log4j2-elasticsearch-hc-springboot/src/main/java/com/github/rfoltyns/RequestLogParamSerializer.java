package com.github.rfoltyns;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;

public class RequestLogParamSerializer extends JsonSerializer<Object[]> {

    private UnwrappingBeanSerializer requestLogSerializer;

    @Override
    public void serialize(Object[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        for (Object param : value) {
            if (param instanceof RequestLog) {
                requestLogUnwrappingSerializer(serializers).serialize(param, gen, serializers);
            }
        }
    }

    private UnwrappingBeanSerializer requestLogUnwrappingSerializer(SerializerProvider serializers) throws JsonMappingException {

        if (requestLogSerializer == null) {

            JsonSerializer<Object> componentSerializer = serializers.findValueSerializer(RequestLog.class);
            requestLogSerializer = (UnwrappingBeanSerializer) componentSerializer.unwrappingSerializer(NameTransformer.NOP);

        }

        return requestLogSerializer;

    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }
}
