package io.github.lunbun.quasar.client.engine.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class MessageBus {
    private static final List<System> systems = new ObjectArrayList<>();

    public static void registerSystem(System system) {
        systems.add(system);
    }

    public static void postMessage(MessageData data) {
        for (System system : systems) {
            system.handleMessage(data);
        }
    }

    public static void postMessage(int type) {
        MessageBus.postMessage(new MessageData(type));
    }

    static {
        new ThreadedSystemManager();
    }
}
