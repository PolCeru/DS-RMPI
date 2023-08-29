package it.polimi.ds.lib;

public interface MiddlewareAPI {
    public void start();

    public void sendMessage(byte[] payload);

    public byte[] readMessage();

    public byte[] nextStableMessage();
}
