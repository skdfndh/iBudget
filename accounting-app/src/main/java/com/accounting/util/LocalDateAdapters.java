package com.accounting.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

public final class LocalDateAdapters {
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static JsonSerializer<LocalDate> localDateSerializer() {
        return (src, typeOfSrc, context) -> src == null ? null : new JsonPrimitive(src.format(DATE_FMT));
    }

    public static JsonDeserializer<LocalDate> localDateDeserializer() {
        return (json, typeOfT, context) -> {
            if (json == null || json.getAsString() == null || json.getAsString().isEmpty()) return null;
            try {
                return LocalDate.parse(json.getAsString(), DATE_FMT);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        };
    }

    public static JsonSerializer<LocalDateTime> localDateTimeSerializer() {
        return (src, typeOfSrc, context) -> src == null ? null : new JsonPrimitive(src.format(DATETIME_FMT));
    }

    public static JsonDeserializer<LocalDateTime> localDateTimeDeserializer() {
        return (json, typeOfT, context) -> {
            if (json == null || json.getAsString() == null || json.getAsString().isEmpty()) return null;
            try {
                return LocalDateTime.parse(json.getAsString(), DATETIME_FMT);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        };
    }
}
