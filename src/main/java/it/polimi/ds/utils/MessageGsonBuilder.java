package it.polimi.ds.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ds.communication.message.BasicMessage;
import it.polimi.ds.communication.message.MessageType;
import it.polimi.ds.vsync.view.message.ViewChangeType;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;

import java.time.LocalDateTime;

public class MessageGsonBuilder {
    private final static String basicMessagePackage = "it.polimi.ds.communication.message.";
    private final static String viewManagerMessagePackage = "it.polimi.ds.vsync.view.message.";
    private final static String messageField = "messageType";

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    public MessageGsonBuilder registerBasicMessageAdapter() {
        RuntimeTypeAdapterFactory<BasicMessage> messageRuntimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory.of(BasicMessage.class, messageField, true);

        for (MessageType type : MessageType.values()) {
            try {
                //noinspection unchecked
                messageRuntimeTypeAdapterFactory.registerSubtype((Class<? extends BasicMessage>) Class.forName(
                        basicMessagePackage + type.className), type.name());
            } catch (ClassNotFoundException e) {
                System.err.println("MessageGson#registerMessageAdapter(): class not found for type " + type + ": " +
                        basicMessagePackage + type.className);
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
        return gsonBuilder.serializeNulls().create();
    }

    public MessageGsonBuilder registerViewMessageAdapter() {
        RuntimeTypeAdapterFactory<ViewManagerMessage> messageRuntimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory.of(ViewManagerMessage.class, messageField, true);

        for (ViewChangeType type : ViewChangeType.values()) {
            try {
                //noinspection unchecked
                messageRuntimeTypeAdapterFactory.registerSubtype((Class<? extends ViewManagerMessage>) Class.forName(
                        viewManagerMessagePackage + type.className), type.name());
            } catch (ClassNotFoundException e) {
                System.err.println("MessageGson#registerMessageAdapter(): class not found for type " + type + ": " +
                        viewManagerMessagePackage + type.className);
                throw new RuntimeException(e);
            }
        }
        gsonBuilder.registerTypeAdapterFactory(messageRuntimeTypeAdapterFactory);
        return this;
    }
}
