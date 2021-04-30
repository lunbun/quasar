package io.github.lunbun.quasar.client.engine.message;

public abstract class ThreadedSystem extends System {
    public ThreadedSystem() {
        ThreadedSystemManager.registerSystem(this);
    }
}
