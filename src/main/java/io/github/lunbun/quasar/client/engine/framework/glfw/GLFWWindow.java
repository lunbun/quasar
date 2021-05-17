package io.github.lunbun.quasar.client.engine.framework.glfw;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class GLFWWindow {
    public static final Map<Integer, Integer> glfwWindowHints = new Int2IntArrayMap();

    public static void disableClientAPI() {
        glfwWindowHints.put(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
    }

    public static void setResizable(boolean resizable) {
        glfwWindowHints.put(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    public static boolean windowShouldClose(long window) {
        return GLFW.glfwWindowShouldClose(window);
    }

    public static void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public static double getTime() {
        return GLFW.glfwGetTime();
    }
}
