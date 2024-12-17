package com.nervesparks.iris

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import java.io.File
import java.util.Locale
import java.util.UUID

class MainViewModel(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()): ViewModel() {
    companion object {
//        @JvmStatic
//        private val NanosPerSecond = 1_000_000_000.0
    }

    //Variable Declaration Start

    private val tag: String? = this::class.simpleName
    var messages by mutableStateOf(

            listOf<Map<String, String>>(),
        )
        private set
    var user_thread by mutableStateOf(0f)
    var allModels by mutableStateOf(
        listOf(
            mapOf(
                "name" to "Llama-3.2-1B-Instruct-Q6_K_L",
                "source" to "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true",
                "destination" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf"
            ),
            mapOf(
                "name" to "Llama-3.2-3B-Instruct-Q4_K_L",
                "source" to "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true",
                "destination" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf"
            ),
            mapOf(
                "name" to "stablelm-2-1_6b-chat.Q4_K_M.imx",
                "source" to "https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true",
                "destination" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf"
            ),

        )
    )
    private var first by mutableStateOf(
        true
    )
    var message by mutableStateOf("")
        private set
    var userGivenModel by mutableStateOf("")
    var SearchedName by mutableStateOf("")
    private var textToSpeech:TextToSpeech? = null
    var textForTextToSpeech = ""
    var stateForTextToSpeech by mutableStateOf(true)
        private set
    var eot_str = ""
    var toggler by mutableStateOf(false)
    var showModal by  mutableStateOf(true)
    var showAlert by mutableStateOf(false)
    var currentDownloadable: Downloadable? by mutableStateOf(null)
    var loadedModelName = mutableStateOf("");

    //Variable Declaration End

    fun loadExistingModels(directory: File) {
        // List models in the directory that end with .gguf
        directory.listFiles { file -> file.extension == "gguf" }?.forEach { file ->
            val modelName = file.nameWithoutExtension
            if (!allModels.any { it["name"] == modelName }) {
                allModels += mapOf(
                    "name" to modelName,
                    "source" to "local",
                    "destination" to file.name
                )
            }
        }

        // Attempt to find and load the first model that exists in the combined logic
        allModels.find { model ->
            val destinationPath = File(directory, model["destination"].toString())
            destinationPath.exists()
        }?.let { model ->
            val destinationPath = File(directory, model["destination"].toString())
            load(destinationPath.path, userThreads = user_thread.toInt())
            currentDownloadable = Downloadable(
                model["name"].toString(),
                Uri.parse(model["source"].toString()),
                destinationPath
            )
        }
    }

    fun textToSpeech(context: Context) {
        if (!getIsSending()) {
            // If TTS is already initialized, stop it first
            textToSpeech?.stop()

            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.let { txtToSpeech ->
                        txtToSpeech.language = Locale.US
                        txtToSpeech.setSpeechRate(1.0f)

                        // Add a unique utterance ID for tracking
                        val utteranceId = UUID.randomUUID().toString()

                        txtToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                // Reset state when speech is complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onStart(utteranceId: String?) {
                                // Update state to indicate speech is playing
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = false
                                }
                            }
                        })

                        txtToSpeech.speak(
                            textForTextToSpeech,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                    }
                }
            }
        }
    }

    fun stopTextToSpeech() {
        textToSpeech?.apply {
            stop()  // Stops current speech
            shutdown()  // Releases the resources
        }
        textToSpeech = null

        // Reset state to allow restarting
        stateForTextToSpeech = true
    }

    override fun onCleared() {
        textToSpeech?.shutdown()
        super.onCleared()

        viewModelScope.launch {
            try {

                llamaAndroid.unload()

            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            }
        }
    }

    fun send() {
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""

        // Add to messages console.
        if (userMessage != "" && userMessage != " ") {
            if(first){
                addMessage("system", "This is a conversation between User and Iris, a friendly chatbot. Iris is helpful, kind, honest, good at writing, and never fails to answer any requests immediately and with precision.")
                addMessage("user", "Hi")
                addMessage("assistant", "How may I help You?")
                first = false
            }

            addMessage("user", userMessage)


            viewModelScope.launch {
                try {
                    llamaAndroid.send(llamaAndroid.getTemplate(messages))
                        .catch {
                            Log.e(tag, "send() failed", it)
                            addMessage("error", it.message ?: "")
                        }
                        .collect { response ->
                            // Create a new assistant message with the response
                            if (getIsMarked()) {
                                addMessage("codeBlock", response)

                            } else {
                                addMessage("assistant", response)
                            }
                        }
                }
                finally {
                    if (!getIsCompleteEOT()) {
                        trimEOT()
                    }
                }



            }
        }



    }


    suspend fun unload(){
        llamaAndroid.unload()
    }


    fun load(pathToModel: String, userThreads: Int)  {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Log.e(tag, "load() failed", exc)
            }
            try {
                var modelName = pathToModel.split("/")
                loadedModelName.value = modelName.last()
                showModal= false
                showAlert = true
                llamaAndroid.load(pathToModel, userThreads)
                showAlert = false

            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                addMessage("error", exc.message ?: "")
            }
            showModal = false
            eot_str = llamaAndroid.send_eot_str()
        }
    }
    private fun addMessage(role: String, content: String) {
        val newMessage = mapOf("role" to role, "content" to content)

        messages = if (messages.isNotEmpty() && messages.last()["role"] == role) {
            val lastMessageContent = messages.last()["content"] ?: ""
            val updatedContent = "$lastMessageContent$content"
            val updatedLastMessage = messages.last() + ("content" to updatedContent)
            messages.toMutableList().apply {
                set(messages.lastIndex, updatedLastMessage)
            }
        } else {
            messages + listOf(newMessage)
        }
    }

    private fun trimEOT() {
        if (messages.isEmpty()) return
        val lastMessageContent = messages.last()["content"] ?: ""
        // Only slice if the content is longer than the EOT string
        if (lastMessageContent.length < eot_str.length) return

        val updatedContent = lastMessageContent.slice(0..(lastMessageContent.length-eot_str.length))
        val updatedLastMessage = messages.last() + ("content" to updatedContent)
        messages = messages.toMutableList().apply {
            set(messages.lastIndex, updatedLastMessage)
        }
        messages.last()["content"]?.let { Log.e(tag, it) }
    }

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }
    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf(

        )
        first = true
    }

    fun log(message: String) {
//        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llamaAndroid.getIsSending()
    }

    private fun getIsMarked(): Boolean {
        return llamaAndroid.getIsMarked()
    }

    fun getIsCompleteEOT(): Boolean{
        return llamaAndroid.getIsCompleteEOT()
    }

    fun stop() {
        llamaAndroid.stopTextGeneration()
    }

}


//    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
//        viewModelScope.launch {
//            try {
//                val start = System.nanoTime()
//                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
//                val end = System.nanoTime()
//
//                messages += warmupResult
//
//                val warmup = (end - start).toDouble() / NanosPerSecond
//                messages += "Warm up time: $warmup seconds, please wait..."
//
//                if (warmup > 5.0) {
//                    messages += "Warm up took too long, aborting benchmark"
//                    return@launch
//                }
//
//                messages += llamaAndroid.bench(512, 128, 1, 3)
//            } catch (exc: IllegalStateException) {
//                Log.e(tag, "bench() failed", exc)
//                messages += exc.message!!
//            }
//        }
//    }


