package com.nervesparks.iris

import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()): ViewModel() {
    companion object {
//        @JvmStatic
//        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(
        listOf<Map<String, String>>(

        )
    )
        private set

    private var first by mutableStateOf(
        true
    )

    var message by mutableStateOf("")
        private set

    var showModal by  mutableStateOf(true)
    var showAlert by mutableStateOf(false)
    override fun onCleared() {
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
                first = false
            }
            addMessage("user", userMessage)
            val text = parseTemplateJson(messages)+"assistant \n"
            viewModelScope.launch {
                llamaAndroid.send(text)
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

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Log.e(tag, "load() failed", exc)
            }
            try {
                showAlert = true
                llamaAndroid.load(pathToModel)
                showAlert = false

            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                addMessage("error", exc.message ?: "")
            }
            showModal = false
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

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }

    private fun parseTemplateJson(chatData: List<Map<String, String>> ):String{
        var chatStr = ""
        for (data in chatData){
            val role = data["role"]
            val content = data["content"]
            if (role != "log"){
                chatStr += "$role \n$content \n"
            }

        }
        return chatStr
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
        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llamaAndroid.getIsSending()
    }

    private fun getIsMarked(): Boolean {
        return llamaAndroid.getIsMarked()
    }

    fun stop() {
        llamaAndroid.stopTextGeneration()
    }
}
