package android.llama.cpp

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LLamaAndroid {
    private val tag: String? = this::class.simpleName
    private var stopGeneration: Boolean = false
    private var nativeLibraryLoaded: Boolean = false
    //private var model_eot_str: String = ""

    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    @Volatile private var modelHandleCache: Long = 0L
    @Volatile private var contextHandleCache: Long = 0L
    @Volatile private var batchHandleCache: Long = 0L
    @Volatile private var samplerHandleCache: Long = 0L

    private val _isSending = mutableStateOf(false)
    private val isSending: Boolean by _isSending

    private val _isMarked = mutableStateOf(false)
    private val isMarked: Boolean by _isMarked

    private val _isCompleteEOT = mutableStateOf(true)
    private val isCompleteEOT: Boolean by _isCompleteEOT

    fun getIsSending(): Boolean {
        return isSending
    }


    fun getIsMarked(): Boolean {
        return isMarked
    }

    fun getIsCompleteEOT(): Boolean {
        return isCompleteEOT
    }

    fun isNativeLibraryLoaded(): Boolean {
        return nativeLibraryLoaded
    }

    // Synchronous library loading check
    fun ensureLibraryLoaded(): Boolean {
        if (nativeLibraryLoaded) {
            return true
        }
        
        try {
            Log.d(tag, "Attempting synchronous library load...")
            System.loadLibrary("llama-android")
            Log.d(tag, "Synchronously loaded llama-android library")
            nativeLibraryLoaded = true
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(tag, "Synchronous library load failed: ${e.message}")
            Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
            return false
        } catch (e: Exception) {
            Log.e(tag, "Synchronous library load error: ${e.message}")
            Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
            return false
        }
    }

    fun stopTextGeneration() {
        _isSending.value = false

        stopGeneration = true
        _isMarked.value = false
    }


    val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")

            try {
                // Load the native library
                Log.d(tag, "Attempting to load llama-android library...")
                System.loadLibrary("llama-android")
                Log.d(tag, "Successfully loaded llama-android library")
                nativeLibraryLoaded = true

                // Set llama log handler to Android
                log_to_android()
                backend_init(false)
                // Push a higher desired context length for capable devices
                runCatching { set_context_length(context_size) }

                Log.d(tag, system_info())
            } catch (e: UnsatisfiedLinkError) {
                Log.e(tag, "Failed to load native library: ${e.message}")
                Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
                // Continue without native functionality
                // The app will work in CPU-only mode
                nativeLibraryLoaded = false
            } catch (e: Exception) {
                Log.e(tag, "Error initializing native code: ${e.message}")
                Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
                // Continue without native functionality
                nativeLibraryLoaded = false
            }

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception in Llm-RunLoop", exception)
                Log.e(tag, "Stack trace: ${exception.stackTraceToString()}")
            }
        }
    }.asCoroutineDispatcher()

    private val nlen: Int = 256
    // Allow larger contexts on devices with ample RAM; native defaults to 2048 for GPU stability
    private val context_size: Int = 8192

    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long, userThreads: Int): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun set_context_length(nCtx: Int)
    private external fun set_backend(backend: String): Boolean
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(top_p: Float, top_k: Int, temp: Float): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(
        context: Long,
        model: Long,
        batch: Long,
        pp: Int,
        tg: Int,
        pl: Int,
        nr: Int
    ): String

    private external fun system_info(): String
    private external fun set_backend_search_dir(dir: String)
    private external fun set_gpu_layers(ngl: Int)
    private external fun is_offload_zero(): Boolean
    external fun set_strip_think(enable: Boolean)
    external fun get_offload_counts(): IntArray
    external fun get_kv_size_bytes(): Long

    private external fun completion_init(
        context: Long,
        batch: Long,
        text: String,
        nLen: Int
    ): Int

    private external fun oaicompat_completion_param_parse(
        allmessages: Array<Map<String, String>>,
        model: Long,
        chatFormat: String
    ): String

    private external fun completion_loop(
        context: Long,
        batch: Long,
        sampler: Long,
        nLen: Int,
        ncur: IntVar
    ): String?

    private external fun kv_cache_clear(context: Long)

    private external fun get_eot_str(model: Long): String
    private external fun count_tokens(model: Long, text: String): Int
    private external fun get_embeddings(model: Long, text: String): FloatArray
    private external fun quantizeNative(inputPath: String, outputPath: String, quantizeType: String): Int

    private external fun getMemoryUsageNative(context: Long): Long
    private external fun set_verbose_tokens(enable: Boolean)
    private external fun export_diag(): String



    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String {
        return withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    Log.d(tag, "bench(): $state")
                    bench_model(state.context, state.model, state.batch, pp, tg, pl, nr)
                }

                else -> throw IllegalStateException("No model loaded")
            }
        }
    }

    suspend fun getOffloadCounts(): IntArray {
        return withContext(runLoop) { get_offload_counts() }
    }

    suspend fun load(pathToModel: String, userThreads: Int, topK: Int, topP: Float, temp: Float, gpuLayers: Int = -1){
        if (!nativeLibraryLoaded) {
            // Best-effort synchronous load to avoid UnsatisfiedLinkError on first JNI call
            ensureLibraryLoaded()
        }
        withContext(runLoop) {
            when (threadLocalState.get()) {
                is State.Idle -> {
                    // Configure GPU offload preference before loading the model
                    set_gpu_layers(gpuLayers)
                    val model = load_model(pathToModel)
                    if (model == 0L)  throw IllegalStateException("load_model() failed")

                    // Configure native stream filtering (default on). UI can still collapse/hide thinking tokens.
                    set_strip_think(true)

                    val context = new_context(model, userThreads)
                    if (context == 0L) throw IllegalStateException("new_context() failed")

                    val batch = new_batch(1024, 0, 1)
                    if (batch == 0L) throw IllegalStateException("new_batch() failed")

                    val sampler = new_sampler(top_k = topK, top_p = topP, temp = temp)
                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")


                    val modelEotStr = get_eot_str(model)
                    if (modelEotStr == "") throw IllegalStateException("eot_fetch() failed")

                    val counts = get_offload_counts()
                    val offMsg = if (counts.size == 2) "offloaded ${counts[0]}/${counts[1]}" else "offload n/a"
                    Log.i(tag, "Loaded model $pathToModel ($offMsg)")
                    threadLocalState.set(State.Loaded(model, context, batch, sampler, modelEotStr))
                    modelHandleCache = model
                    contextHandleCache = context
                    batchHandleCache = batch
                    samplerHandleCache = sampler
                }
                else -> {
                    throw IllegalStateException("Model already loaded")
                }
            }
        }
    }

    fun setBackendSearchDir(dir: String) {
        if (!nativeLibraryLoaded) {
            ensureLibraryLoaded()
        }
        // No need to hop to runLoop; setting a static path is cheap
        set_backend_search_dir(dir)
    }


    // Legacy template function - now delegated to TemplateRegistry
    suspend fun getTemplate(messages: List<Map<String, String>>, chatFormat: String = "CHATML"): String {
        // Fallback to simple CHATML format if TemplateRegistry not available
        val sb = StringBuilder()
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "system" -> "system"
                "assistant" -> "assistant"
                else -> "user"
            }
            sb.append("<|im_start|>").append(tag).append("\n")
            sb.append(m["content"] ?: "").append("<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    suspend fun countTokens(text: String): Int {
        var res = 0
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    res = count_tokens(state.model, text)
                }
                else -> {}
            }
        }
        return res
    }

    suspend fun getEmbeddings(text: String): FloatArray {
        var res = floatArrayOf()
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    res = get_embeddings(state.model, text)
                }
                else -> {}
            }
        }
        return res
    }

    suspend fun quantize(inputPath: String, outputPath: String, quantizeType: String): Int {
        var res = -1
        withContext(runLoop) {
            res = quantizeNative(inputPath, outputPath, quantizeType)
        }
        return res
    }
    
    suspend fun setBackend(backend: String): Boolean {
        var success = false
        withContext(runLoop) {
            success = set_backend(backend)
        }
        return success
    }

    fun setVerboseTokens(enable: Boolean) {
        if (!nativeLibraryLoaded) return
        set_verbose_tokens(enable)
    }

    suspend fun exportDiag(): String {
        return withContext(runLoop) { export_diag() }
    }
    
    suspend fun getMemoryUsage(): Long {
        var res = 0L
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    res = getMemoryUsageNative(state.context)
                }
                else -> {}
            }
        }
        return res
    }

    suspend fun send(message: String): Flow<String> = flow {
        stopGeneration = false
        _isSending.value = true
        when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                val ncur = IntVar(completion_init(state.context, state.batch, message, nlen))
                var end_token_store = ""
                var chat_len = 0
                val buffer = StringBuilder()
                var lastEmitNs = System.nanoTime()
                while (chat_len <= nlen && ncur.value < context_size && !stopGeneration) {
                    _isSending.value = true
                    val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
                    chat_len += 1
                    if (str == "```" || str == "``") {
                        _isMarked.value = !_isMarked.value
                    }
                    if (str == null) {
                        if (buffer.isNotEmpty()) {
                            emit(buffer.toString())
                            buffer.clear()
                        }
                        _isSending.value = false
                        _isCompleteEOT.value = true
                        break
                    }
                    end_token_store = end_token_store+str
                    if((end_token_store.length > state.modelEotStr.length) and end_token_store.contains(state.modelEotStr)){
                        _isSending.value = false
                        _isCompleteEOT.value = false
                        break
                    }
                    if((end_token_store.length/2) > state.modelEotStr.length ){
                        end_token_store = end_token_store.slice(end_token_store.length/2..end_token_store.length-1)
                    }


//                    if (str == "</s>" || str == " User" || str== " user" || str == "user" || str == "<|im_end|>" || str == "\n" +
//                        "                                                                                                    "
//                    ) {
//
//                        _isSending.value = false
//                        break
//
//                    }
                    if (stopGeneration) {
                        break
                    }

                    // Coalesce tokens to reduce recomposition/jank
                    buffer.append(str)
                    val now = System.nanoTime()
                    val elapsedMs = (now - lastEmitNs) / 1_000_000
                    val shouldFlush = elapsedMs >= 30 || str.contains('\n') || buffer.length >= 64
                    if (shouldFlush) {
                        emit(buffer.toString())
                        buffer.clear()
                        lastEmitNs = now
                    }
                }
                if (buffer.isNotEmpty()) {
                    emit(buffer.toString())
                }
                kv_cache_clear(state.context)
            }
            else -> {
                _isSending.value = false
            }
        }
        _isSending.value = false
    }.flowOn(runLoop)



    suspend fun myCustomBenchmark(): Flow<String> = flow {
        try {
            withTimeout(30.seconds) { // Set timeout to 2 minutes
                when (val state = threadLocalState.get()) {
                    is State.Loaded -> {
                        val ncur = IntVar(completion_init(state.context, state.batch, "Write an article on global warming in 1000 words", nlen))
                        while (ncur.value <= nlen) {
                            val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
                            if (str == null) {
                                _isSending.value = false
                                _isCompleteEOT.value = true
                                break
                            }
                            if (stopGeneration) {
                                break
                            }
                            emit(str)
                        }
                        kv_cache_clear(state.context)
                    }
                    else -> {
                        _isSending.value = false
                    }
                }
            }
        } catch (e: Exception) {
            // Handle timeout or any other exceptions if necessary
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                println("Benchmark timed out after 2 minutes.")
            }
        } finally {
            _isSending.value = false
        }
    }.flowOn(runLoop)





    /**
     * Unloads the model and frees resources.
     *
     * This is a no-op if there's no model loaded.
     */
    suspend fun unload() {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    free_context(state.context)
                    free_model(state.model)
                    free_sampler(state.sampler)
                    free_batch(state.batch)

                    threadLocalState.set(State.Idle)
                    modelHandleCache = 0L
                    contextHandleCache = 0L
                    batchHandleCache = 0L
                    samplerHandleCache = 0L
                }
                else -> {}
            }
        }
    }

    fun send_eot_str(): String {

        return when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                state.modelEotStr
            }

            else -> {
                "<|im_end|>"
            }
        }

    }

    // Hardware detection functions for GPU acceleration
    external fun getAvailableBackends(): String
    external fun getOptimalBackend(): String
    external fun getGpuInfo(): String
    external fun isAdrenoGpu(): Boolean
    
    // Comparative benchmark function
    external fun runComparativeBenchmark(model: Long, context: Long, batch: Long, sampler: Long): String
    
    // Getter functions for current model state
    fun getModel(): Long = modelHandleCache
    
    fun getContext(): Long = contextHandleCache
    
    fun getBatch(): Long = batchHandleCache
    
    fun getSampler(): Long = samplerHandleCache

    companion object {
        private class IntVar(value: Int) {
            @Volatile
            var value: Int = value
                private set

            fun inc() {
                synchronized(this) {
                    value += 1
                }
            }
        }

        private sealed interface State {
            data object Idle: State
            data class Loaded(val model: Long, val context: Long, val batch: Long, val sampler: Long , val modelEotStr:String): State
        }

        // Enforce only one instance of Llm.
        private val _instance: LLamaAndroid = LLamaAndroid()

        fun instance(): LLamaAndroid = _instance
    }
}
