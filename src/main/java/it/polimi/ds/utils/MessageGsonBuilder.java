package it.polimi.ds.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ds.communication.BasicMessage;
import it.polimi.ds.communication.MessageType;

import java.time.LocalDateTime;

public class MessageGsonBuilder {
    private final static String messagePackage = "it.polimi.ds.communication.";
    private final static String messageField = "messageType";

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    public MessageGsonBuilder registerMessageAdapter() {
        RuntimeTypeAdapterFactory<BasicMessage> messageRuntimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory.of(BasicMessage.class, messageField, true);

        for (MessageType type : MessageType.values()) {
            try {
                //noinspection unchecked
                messageRuntimeTypeAdapterFactory.registerSubtype((Class<? extends BasicMessage>) Class.forName(
                        messagePackage + type.getClassName()), type.name());
            } catch (ClassNotFoundException e) {
                System.err.println("MesssageGson#registerMessageAdapter(): class not found for type " + type + ": " +
                        messagePackage + type.getClassName());
                throw new RuntimeException(e);
            }
        }
        gsonBuilder.registerTypeAdapterFactory(messageRuntimeTypeAdapterFactory);
        return this;
    }

    public MessageGsonBuilder registerLocalDateTimeAdapter() {
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        return this;
    }
    public Gson create(){
        return gsonBuilder.create();
    }

}
