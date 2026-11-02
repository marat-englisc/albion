package com.marat.albion;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import com.marat.albion.data.translation.LlamaBridge;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Framework-only runner: validates the kept JNI API against the actual minified release APK. */
public final class LlamaNativeSmokeRunner extends Instrumentation {
    private String modelPath;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        modelPath = arguments == null ? null : arguments.getString("primaryModelPath");
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        long handle = 0;
        int status = Activity.RESULT_CANCELED;
        try {
            if (modelPath == null || !new File(modelPath).isFile()) {
                throw new IllegalArgumentException("Pass primaryModelPath with a real Qwen3.5 GGUF");
            }
            LlamaBridge.INSTANCE.requireAvailable();
            handle = LlamaBridge.INSTANCE.nativeLoad(modelPath, 4);
            if (handle == 0) throw new IllegalStateException("Native model did not load");
            String prompt = "<|im_start|>system\nПереведи с английского на русский. Ответ — только перевод.<|im_end|>\n"
                    + "<|im_start|>user\nGood morning!<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n";
            AtomicBoolean streamed = new AtomicBoolean(false);
            byte[] output = LlamaBridge.INSTANCE.nativeGenerate(handle, 1, 1, prompt.getBytes(StandardCharsets.UTF_8), 96, 0f, 0, false,
                    new LlamaBridge.TokenCallback() {
                        @Override public boolean onTokens(byte[] utf8) {
                            if (utf8.length > 0) streamed.set(true);
                            return true;
                        }
                    });
            String translation = new String(output, StandardCharsets.UTF_8);
            if (!streamed.get() || !translation.toLowerCase(Locale.ROOT).contains("утр")) {
                throw new AssertionError("Streaming/translation failed: " + translation);
            }
            result.putBoolean("success", true);
            result.putString("translation", translation);
            result.putString("stream", "Release JNI: load, generate, stream and unload passed\n");
            status = Activity.RESULT_OK;
        } catch (Throwable error) {
            result.putBoolean("success", false);
            result.putString("stream", error.toString() + "\n");
        } finally {
            if (handle != 0) LlamaBridge.INSTANCE.nativeUnload(handle);
            finish(status, result);
        }
    }
}
