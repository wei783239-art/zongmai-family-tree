package com.zongmai.familytree;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StrictLongDeserializer  extends JsonDeserializer<Long> {

    @Override 
    public Long deserialize(
              JsonParser parser,
              DeserializationContext context) throws IOException {
          
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw JsonMappingException.from(parser, "编号必须是整数");
        }

        return parser.getLongValue();
    }

}