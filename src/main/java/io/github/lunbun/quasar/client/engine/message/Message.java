package io.github.lunbun.quasar.client.engine.message;

public class Message {
    private static int messageIndex = 0;

    public static int nextMessageIndex() {
        return messageIndex++;
    }
}
