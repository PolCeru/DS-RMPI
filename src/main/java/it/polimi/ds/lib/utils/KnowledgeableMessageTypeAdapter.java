package it.polimi.ds.lib.utils;

import com.google.gson.*;
import it.polimi.ds.lib.vsync.KnowledgeableMessage;
import it.polimi.ds.lib.vsync.KnowledgeableMessageType;
import it.polimi.ds.lib.vsync.VSyncMessage;
import it.polimi.ds.lib.vsync.view.message.ViewManagerMessage;

import java.lang.reflect.Type;

public class KnowledgeableMessageTypeAdapter implements JsonSerializer<KnowledgeableMessage>, JsonDeserializer<KnowledgeableMessage> {
    private static final String messageTypeField = "knowledgeableMessageType";

    @Override
    public KnowledgeableMessage deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        KnowledgeableMessageType messageType = KnowledgeableMessageType.valueOf(jsonObject.get(messageTypeField).getAsString());
        if (messageType == KnowledgeableMessageType.VIEW) {
            return jsonDeserializationContext.deserialize(jsonElement, ViewManagerMessage.class);
        } else {
            return jsonDeserializationContext.deserialize(jsonElement, VSyncMessage.class);
        }
    }

    @Override
    public JsonElement serialize(KnowledgeableMessage knowledgeableMessage, Type type, JsonSerializationContext jsonSerializationContext) {
        if (knowledgeableMessage.knowledgeableMessageType == KnowledgeableMessageType.VIEW) {
            return jsonSerializationContext.serialize(knowledgeableMessage, ViewManagerMessage.class);
        } else {
            return jsonSerializationContext.serialize(knowledgeableMessage, VSyncMessage.class);
        }
    }
}
