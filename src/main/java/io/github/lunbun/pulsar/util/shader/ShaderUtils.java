package io.github.lunbun.pulsar.util.shader;

import io.github.lunbun.pulsar.util.misc.FileUtils;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;
import java.util.Objects;

// https://github.com/Naitsirc98/Vulkan-Tutorial-Java/blob/ff0567a6635322d0413196f2ceffe338eef52bdb/src/main/java/javavulkantutorial/ShaderSPIRVUtils.java#L68
public final class ShaderUtils {
    private ShaderUtils() { }

    public static SPIRV compileShaderFile(String shaderFile, ShaderType ShaderType) {
        return compileShaderAbsoluteFile(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource(shaderFile)).toExternalForm(), ShaderType);
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderType ShaderType) {
        try {
            String source = FileUtils.readFile(shaderFile);
            return compileShader(shaderFile, source, ShaderType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SPIRV compileShader(String filename, String source, ShaderType ShaderType) {
        long compiler = Shaderc.shaderc_compiler_initialize();

        if(compiler == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        long result = Shaderc.shaderc_compile_into_spv(compiler, source, ShaderType.type, filename, "main", MemoryUtil.NULL);

        if(result == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n " + Shaderc.shaderc_result_get_error_message(result));
        }

        Shaderc.shaderc_compiler_release(compiler);

        return new SPIRV(result, Shaderc.shaderc_result_get_bytes(result));
    }

}
