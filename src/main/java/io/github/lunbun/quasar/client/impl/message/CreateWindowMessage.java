package io.github.lunbun.quasar.client.impl.message;

import io.github.lunbun.quasar.client.engine.message.MessageData;

public final class CreateWindowMessage extends MessageData {
    public final long handle;

    public CreateWindowMessage(long handle) {
        super(MessageImpl.CREATE_WINDOW);
        this.handle = handle;
    }
}
