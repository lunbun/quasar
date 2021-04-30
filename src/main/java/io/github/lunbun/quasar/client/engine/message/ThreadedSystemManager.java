package io.github.lunbun.quasar.client.engine.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class ThreadedSystemManager extends System {
    private static final List<ThreadedSystemWrapper> systems = new ObjectArrayList<>();

    public static void registerSystem(ThreadedSystem system) {
        ThreadedSystemWrapper wrapper = new ThreadedSystemWrapper(system);
        systems.add(wrapper);
        wrapper.start();
    }

    @Override
    public void handleMessage(MessageData data) {
        for (ThreadedSystemWrapper system : systems) {
            system.queue.add(data);
        }
    }
}
