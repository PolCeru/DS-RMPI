package it.polimi.ds.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type srcType,
                                 JsonSerializationContext context) {

        return new JsonPrimitive(String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                     JsonDeserializationContext context) throws JsonParseException {

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(json.getAsLong()), ZoneOffset.UTC);
    }
}