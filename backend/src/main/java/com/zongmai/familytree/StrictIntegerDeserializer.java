package com.zongmai.familytree;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override 
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "字段必须是整数");
        }

        return parser.getIntValue();
    }
}