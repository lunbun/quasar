package io.github.lunbun.quasar.client.engine.message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadedSystemWrapper extends Thread {
    private final ThreadedSystem system;
    public final BlockingQueue<MessageData> queue;

    public ThreadedSystemWrapper(ThreadedSystem system) {
        this.system = system;
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        super.run();
        try {
            while (true) {
                MessageData msg;
                while ((msg = queue.poll()) != null) {
                    this.system.handleMessage(msg);
                }
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
