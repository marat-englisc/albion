#include <android/log.h>
#include <jni.h>
#include <algorithm>
#include <atomic>
#include <stdexcept>
#include <string>
#include <vector>
#include "whisper.h"

// Offline speech recognition for conversation practice: whisper.cpp on the same ggml runtime as Qwen.
namespace {
struct Recognizer {
    whisper_context *context = nullptr;
    std::atomic<int64_t> request{0};
    std::atomic<int64_t> cancelled{-1};
    ~Recognizer() { if (context) whisper_free(context); }
};

void log_message(ggml_log_level level, const char *text, void *) {
    if (level == GGML_LOG_LEVEL_ERROR)
        __android_log_print(ANDROID_LOG_ERROR, "AlbionSTT", "%s", text);
}
bool aborted(void *data) {
    auto *recognizer = static_cast<Recognizer *>(data);
    return recognizer->cancelled.load() == recognizer->request.load();
}
void fail(JNIEnv *env, const char *message) {
    if (!env->ExceptionCheck()) {
        jclass type = env->FindClass("java/lang/IllegalStateException");
        if (type) { env->ThrowNew(type, message); env->DeleteLocalRef(type); }
    }
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_marat_albion_data_speech_WhisperBridge_nativeLoad(JNIEnv *env, jobject, jstring path) {
    try {
        whisper_log_set(log_message, nullptr);
        const char *filename = env->GetStringUTFChars(path, nullptr);
        if (!filename) return 0;
        auto params = whisper_context_default_params();
        params.use_gpu = false;
        params.flash_attn = true;
        auto *context = whisper_init_from_file_with_params(filename, params);
        env->ReleaseStringUTFChars(path, filename);
        if (!context) throw std::runtime_error("Cannot load Whisper model");
        auto *recognizer = new Recognizer();
        recognizer->context = context;
        return reinterpret_cast<jlong>(recognizer);
    } catch (const std::exception &error) { fail(env, error.what()); return 0; }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_marat_albion_data_speech_WhisperBridge_nativeTranscribe(
        JNIEnv *env, jobject, jlong handle, jlong request, jfloatArray pcm, jint threads, jstring prompt) {
    try {
        if (!handle) throw std::runtime_error("Whisper model is not loaded");
        auto *recognizer = reinterpret_cast<Recognizer *>(handle);
        recognizer->request.store(request);
        if (aborted(recognizer)) throw std::runtime_error("Recognition cancelled");
        const auto count = env->GetArrayLength(pcm);
        std::vector<float> samples(static_cast<size_t>(count));
        env->GetFloatArrayRegion(pcm, 0, count, samples.data());
        const char *hint = prompt ? env->GetStringUTFChars(prompt, nullptr) : nullptr;
        const std::string initial = hint ? hint : "";
        if (hint) env->ReleaseStringUTFChars(prompt, hint);

        auto params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.n_threads = std::clamp(static_cast<int>(threads), 1, 6);
        params.language = "en";
        params.translate = false;
        params.no_context = true;
        params.no_timestamps = true;
        params.single_segment = false;
        params.print_special = false;
        params.print_progress = false;
        params.print_realtime = false;
        params.print_timestamps = false;
        params.suppress_blank = true;
        params.suppress_nst = true;
        params.temperature = 0.f;
        // Previous replies prime names and topic words that a learner is likely to say.
        params.initial_prompt = initial.empty() ? nullptr : initial.c_str();
        params.abort_callback = aborted;
        params.abort_callback_user_data = recognizer;
        if (whisper_full(recognizer->context, params, samples.data(), count) != 0)
            throw std::runtime_error(aborted(recognizer) ? "Recognition cancelled" : "Recognition failed");
        std::string text;
        const int segments = whisper_full_n_segments(recognizer->context);
        for (int i = 0; i < segments; ++i) text += whisper_full_get_segment_text(recognizer->context, i);
        // Bytes, not NewStringUTF: Whisper may emit 4-byte UTF-8 that JNI modified UTF-8 rejects.
        auto result = env->NewByteArray(static_cast<jsize>(text.size()));
        if (result) env->SetByteArrayRegion(result, 0, static_cast<jsize>(text.size()), reinterpret_cast<const jbyte *>(text.data()));
        return result;
    } catch (const std::exception &error) { fail(env, error.what()); return nullptr; }
}

extern "C" JNIEXPORT void JNICALL
Java_com_marat_albion_data_speech_WhisperBridge_nativeCancel(JNIEnv *, jobject, jlong handle, jlong request) {
    if (handle) reinterpret_cast<Recognizer *>(handle)->cancelled.store(request);
}
extern "C" JNIEXPORT void JNICALL
Java_com_marat_albion_data_speech_WhisperBridge_nativeUnload(JNIEnv *, jobject, jlong handle) {
    delete reinterpret_cast<Recognizer *>(handle);
}
