#include <android/log.h>
#include <jni.h>
#include <algorithm>
#include <atomic>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>
#include "llama.h"

namespace {
constexpr int BATCH_SIZE = 64;
// Slot 0 keeps a conversation between turns; slot 1 serves short one-off tasks (translation, corrections).
constexpr int SLOTS = 2;
constexpr int SLOT_BUDGET[SLOTS] = {4096, 2048};
constexpr int CONTEXT_SIZE = SLOT_BUDGET[0] + SLOT_BUDGET[1];
struct Engine {
    llama_model *model = nullptr;
    llama_context *context = nullptr;
    std::atomic<int64_t> request{0};
    std::atomic<int64_t> cancelled{-1};
    // Tokens whose state the slot holds; empty when the slot must be rebuilt from scratch.
    std::vector<llama_token> cached[SLOTS];
    ~Engine() {
        if (context) llama_free(context);
        if (model) llama_model_free(model);
    }
};
using Batch = std::unique_ptr<llama_batch_ext, decltype(&llama_batch_ext_free)>;
using Sampler = std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>;
std::once_flag backend_once;

void log_message(ggml_log_level level, const char *text, void *) {
    if (level == GGML_LOG_LEVEL_ERROR || level == GGML_LOG_LEVEL_WARN)
        __android_log_print(level == GGML_LOG_LEVEL_ERROR ? ANDROID_LOG_ERROR : ANDROID_LOG_WARN,
                            "AlbionLLM", "%s", text);
}
bool aborted(void *data) {
    auto *engine = static_cast<Engine *>(data);
    return engine->cancelled.load() == engine->request.load();
}
void fail(JNIEnv *env, const char *message) {
    if (!env->ExceptionCheck()) {
        jclass type = env->FindClass("java/lang/IllegalStateException");
        if (type) { env->ThrowNew(type, message); env->DeleteLocalRef(type); }
    }
}
Engine *engine_of(jlong handle) {
    if (!handle) throw std::runtime_error("Model is not loaded");
    return reinterpret_cast<Engine *>(handle);
}
std::string bytes_of(JNIEnv *env, jbyteArray bytes) {
    const auto count = env->GetArrayLength(bytes);
    std::string result(static_cast<size_t>(count), '\0');
    env->GetByteArrayRegion(bytes, 0, count, reinterpret_cast<jbyte *>(result.data()));
    return result;
}
jbyteArray java_bytes(JNIEnv *env, const std::string &bytes) {
    auto result = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (result) env->SetByteArrayRegion(result, 0, static_cast<jsize>(bytes.size()),
                                      reinterpret_cast<const jbyte *>(bytes.data()));
    return result;
}
// Byte fallback tokens can split a UTF-8 codepoint, including a four-byte emoji.
bool complete_utf8(const std::string &text) {
    size_t i = 0;
    while (i < text.size()) {
        const auto c = static_cast<unsigned char>(text[i]);
        size_t length = c < 0x80 ? 1 : (c >= 0xc2 && c <= 0xdf) ? 2 :
                        (c >= 0xe0 && c <= 0xef) ? 3 : (c >= 0xf0 && c <= 0xf4) ? 4 : 0;
        if (!length) throw std::runtime_error("Invalid UTF-8 from model");
        if (i + length > text.size()) return false;
        for (size_t j = 1; j < length; ++j)
            if ((static_cast<unsigned char>(text[i+j]) & 0xc0) != 0x80)
                throw std::runtime_error("Invalid UTF-8 from model");
        i += length;
    }
    return true;
}
void add_token(llama_batch_ext *batch, int slot, llama_token token, llama_pos position, bool output) {
    const int index = llama_batch_ext_add_token(batch, slot, token);
    if (index < 0 || !llama_batch_ext_set_pos(batch, index, &position) ||
        !llama_batch_ext_set_output_logits(batch, index, output))
        throw std::runtime_error("Cannot prepare inference batch");
}
void decode(Engine *engine, llama_batch_ext *batch) {
    if (aborted(engine)) throw std::runtime_error("Translation cancelled");
    if (llama_process(engine->context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0)
        throw std::runtime_error(aborted(engine) ? "Translation cancelled" : "Inference failed");
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_marat_albion_data_translation_LlamaBridge_nativeLoad(
        JNIEnv *env, jobject, jstring path, jint threads) {
    try {
        std::call_once(backend_once, [] { llama_log_set(log_message, nullptr); llama_backend_init(); });
        auto engine = std::make_unique<Engine>();
        const char *filename = env->GetStringUTFChars(path, nullptr);
        if (!filename) return 0;
        auto params = llama_model_default_params();
        params.n_gpu_layers = 0;
        params.load_mode = LLAMA_LOAD_MODE_MMAP;
        engine->model = llama_model_load_from_file(filename, params);
        env->ReleaseStringUTFChars(path, filename);
        if (!engine->model) throw std::runtime_error("Cannot load GGUF model");
        char architecture[64]{};
        llama_model_meta_val_str(engine->model, "general.architecture", architecture, sizeof(architecture));
        if (std::string(architecture) != "qwen35") throw std::runtime_error("Expected a Qwen3.5 dense model");
        auto options = llama_context_default_params();
        options.n_ctx = CONTEXT_SIZE;
        options.n_seq_max = SLOTS;
        options.kv_unified = true;
        options.n_batch = BATCH_SIZE;
        options.n_ubatch = BATCH_SIZE;
        options.n_threads = std::clamp(threads, 1, 6);
        options.n_threads_batch = options.n_threads;
        options.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;
        options.abort_callback = aborted;
        options.abort_callback_data = engine.get();
        engine->context = llama_init_from_model(engine->model, options);
        if (!engine->context) throw std::runtime_error("Cannot create model context");
        return reinterpret_cast<jlong>(engine.release());
    } catch (const std::exception &error) { fail(env, error.what()); return 0; }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_marat_albion_data_translation_LlamaBridge_nativeGenerate(
        JNIEnv *env, jobject, jlong handle, jlong request, jint slot, jbyteArray prompt,
        jint max_tokens, jfloat temperature, jint seed, jboolean allow_truncate, jobject callback) {
    try {
        auto *engine = engine_of(handle);
        if (slot < 0 || slot >= SLOTS) throw std::runtime_error("Invalid model slot");
        engine->request.store(request);
        if (aborted(engine)) throw std::runtime_error("Translation cancelled");
        const auto text = bytes_of(env, prompt);
        const auto *vocab = llama_model_get_vocab(engine->model);
        int count = llama_tokenize(vocab, text.data(), static_cast<int>(text.size()), nullptr, 0, true, true);
        if (count >= 0) throw std::runtime_error("Cannot tokenize prompt");
        std::vector<llama_token> tokens(static_cast<size_t>(-count));
        count = llama_tokenize(vocab, text.data(), static_cast<int>(text.size()), tokens.data(),
                               static_cast<int>(tokens.size()), true, true);
        if (count <= 0) throw std::runtime_error("Cannot tokenize prompt");
        tokens.resize(static_cast<size_t>(count));
        const int limit = std::clamp(max_tokens, 16, 1024);
        if (count + limit + 8 > SLOT_BUDGET[slot])
            throw std::runtime_error("Selection is too long for the model context");
        // Qwen3.5 has recurrent state as well as attention memory, and recurrent state cannot be
        // rolled back: reuse the slot only when the new prompt extends everything it holds.
        auto &cached = engine->cached[slot];
        std::vector<llama_token> state;
        if (!cached.empty() && cached.size() < tokens.size() &&
            std::equal(cached.begin(), cached.end(), tokens.begin())) state.swap(cached);
        else llama_memory_seq_rm(llama_get_memory(engine->context), slot, -1, -1);
        cached.clear(); // Until this request completes, the slot contents are unknown.
        Batch batch(llama_batch_ext_init(engine->context), llama_batch_ext_free);
        if (!batch) throw std::runtime_error("Cannot allocate inference batch");
        for (int offset = static_cast<int>(state.size()); offset < count; offset += BATCH_SIZE) {
            llama_batch_ext_clear(batch.get());
            const int size = std::min(BATCH_SIZE, count - offset);
            for (int i = 0; i < size; ++i)
                add_token(batch.get(), slot, tokens[offset+i], offset+i, offset+i == count-1);
            decode(engine, batch.get());
            state.insert(state.end(), tokens.begin() + offset, tokens.begin() + offset + size);
        }
        Sampler sampler(llama_sampler_chain_init(llama_sampler_chain_default_params()), llama_sampler_free);
        if (temperature > 0.f) {
            // Qwen3.5 non-thinking chat settings: varied replies that stay on topic.
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(20));
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 128, 1.05f, 0.f, 0.4f));
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.8f, 1));
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(temperature));
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(static_cast<uint32_t>(seed)));
        } else {
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.05f, 0.f, 0.f));
            llama_sampler_chain_add(sampler.get(), llama_sampler_init_greedy());
        }
        jclass callback_class = env->GetObjectClass(callback);
        jmethodID on_tokens = env->GetMethodID(callback_class, "onTokens", "([B)Z");
        env->DeleteLocalRef(callback_class);
        if (!on_tokens) return nullptr;
        std::string output;
        for (int generated = 0; generated < limit; ++generated) {
            if (aborted(engine)) throw std::runtime_error("Translation cancelled");
            const auto token = llama_sampler_sample(sampler.get(), engine->context, -1);
            if (llama_vocab_is_eog(vocab, token)) {
                if (!complete_utf8(output)) throw std::runtime_error("Incomplete UTF-8 from model");
                cached.swap(state);
                return java_bytes(env, output);
            }
            std::vector<char> piece(256);
            int length = llama_token_to_piece(vocab, token, piece.data(), static_cast<int>(piece.size()), 0, false);
            if (length < 0) {
                piece.resize(static_cast<size_t>(-length));
                length = llama_token_to_piece(vocab, token, piece.data(), static_cast<int>(piece.size()), 0, false);
            }
            if (length < 0) throw std::runtime_error("Cannot decode output token");
            output.append(piece.data(), static_cast<size_t>(length));
            if (complete_utf8(output)) {
                auto bytes = java_bytes(env, output);
                if (!bytes) return nullptr;
                const auto keep_going = env->CallBooleanMethod(callback, on_tokens, bytes);
                env->DeleteLocalRef(bytes);
                if (env->ExceptionCheck()) return nullptr;
                if (!keep_going) throw std::runtime_error("Translation cancelled");
            }
            llama_batch_ext_clear(batch.get());
            add_token(batch.get(), slot, token, count + generated, true);
            decode(engine, batch.get());
            state.push_back(token);
        }
        if (allow_truncate) {
            // A chat reply may stop mid-thought; drop a split codepoint and keep the rest.
            while (!output.empty() && !complete_utf8(output)) output.pop_back();
            cached.swap(state);
            return java_bytes(env, output);
        }
        // Never cache or present a truncated translation as a complete result.
        throw std::runtime_error("Translation exceeded the output limit; select a shorter fragment");
    } catch (const std::exception &error) { fail(env, error.what()); return nullptr; }
}

extern "C" JNIEXPORT void JNICALL
Java_com_marat_albion_data_translation_LlamaBridge_nativeCancel(JNIEnv *, jobject, jlong handle, jlong request) {
    if (handle) reinterpret_cast<Engine *>(handle)->cancelled.store(request);
}
extern "C" JNIEXPORT void JNICALL
Java_com_marat_albion_data_translation_LlamaBridge_nativeUnload(JNIEnv *, jobject, jlong handle) {
    delete reinterpret_cast<Engine *>(handle);
}
