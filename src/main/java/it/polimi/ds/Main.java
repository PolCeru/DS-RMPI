package it.polimi.ds;

import it.polimi.ds.lib.Middleware;

import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();
        middleware.start();
        new Thread(() -> {
            byte[] payload = middleware.retrieveStableMessage();
            System.out.println("Received stable message: " + new String(payload));
        }).start();
        System.out.println("When you want to send a message, digit the text and press enter.\nDigit \"exit\" to exit.");
        Scanner scanner = new Scanner(System.in);
        String s;
        do {
            s = scanner.next();
            if(!Objects.equals(s, "exit")) middleware.sendMessage(s.getBytes());
        } while (!Objects.equals(s, "exit"));
        System.exit(0);
    }
}
