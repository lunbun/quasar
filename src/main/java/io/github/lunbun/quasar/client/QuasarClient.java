package io.github.lunbun.quasar.client;

import io.github.lunbun.quasar.client.engine.message.MessageBus;
import io.github.lunbun.quasar.client.engine.message.System;
import io.github.lunbun.quasar.client.impl.message.MessageImpl;
import io.github.lunbun.quasar.client.impl.system.render.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO LIST:
 * Fix enum .values() stuff
 */
@Environment(EnvType.CLIENT)
public class QuasarClient implements ClientModInitializer {
    private final List<System> systems = new ArrayList<>();

    private void registerSystems() {
        this.systems.add(new RenderSystem());
    }

    @Override
    public void onInitializeClient() {
        this.registerSystems();
        MessageBus.postMessage(MessageImpl.INIT_SYSTEMS);
    }
}
