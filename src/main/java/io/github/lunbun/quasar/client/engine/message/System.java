package io.github.lunbun.quasar.client.engine.message;

public abstract class System {
    public System() {
        if (!(this instanceof ThreadedSystem)) {
            MessageBus.registerSystem(this);
        }
    }

    public abstract void handleMessage(MessageData data);
}
