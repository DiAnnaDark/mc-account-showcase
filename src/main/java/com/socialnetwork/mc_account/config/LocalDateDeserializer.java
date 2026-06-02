package com.socialnetwork.mc_account.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final String NONE_VALUE = "none";

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        String value = parser.getText();

        if (value == null || value.isBlank() || NONE_VALUE.equalsIgnoreCase(value)) {
            return null;
        }

        return LocalDate.parse(value);
    }
}
