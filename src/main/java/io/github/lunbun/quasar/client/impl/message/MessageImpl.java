package io.github.lunbun.quasar.client.impl.message;

import io.github.lunbun.quasar.client.engine.message.Message;

public final class MessageImpl {
    private MessageImpl() { }

    public static final int INIT_SYSTEMS = Message.nextMessageIndex();
    public static final int INIT_WINDOW = Message.nextMessageIndex();
    public static final int CREATE_WINDOW = Message.nextMessageIndex();
    public static final int INIT_VULKAN = Message.nextMessageIndex();
    public static final int CLEANUP = Message.nextMessageIndex();
}
