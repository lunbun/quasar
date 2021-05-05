package io.github.lunbun.pulsar.util.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class FileUtils {
    private static final Map<String, ByteArrayWrapper> filePool = new Object2ObjectOpenHashMap<>();

    public static String readFile(String fileName) throws IOException {
        return new String(readBinaryFile(fileName));
    }

    public static byte[] readBinaryFile(String fileName) throws IOException {
        if (filePool.containsKey(fileName)) {
            // arrays are mutable, have to copy
            byte[] src = filePool.get(fileName).arr;
            byte[] clone = new byte[src.length];
            System.arraycopy(src, 0, clone, 0, src.length);
            return clone;
        } else {
            byte[] buf;
            try {
                buf = Files.readAllBytes(Paths.get(new URI(fileName)));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            filePool.put(fileName, new ByteArrayWrapper(buf));
            return buf;
        }
    }
}
