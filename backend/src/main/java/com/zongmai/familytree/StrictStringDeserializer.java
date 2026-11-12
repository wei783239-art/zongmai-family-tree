package com.zongmai.familytree;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StrictStringDeserializer extends JsonDeserializer<String> {

    @Override public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "家族名称必须是文字");
        }

        return parser.getText();
    }
}