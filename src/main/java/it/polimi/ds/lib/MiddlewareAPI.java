package it.polimi.ds.lib;

public interface MiddlewareAPI {
    public void start();

    public void sendMessage(byte[] payload);

    public byte[] retrieveStableMessage();
}
