package com.example.llama

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llm: Llm = Llm.instance()): ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(listOf<Map<String, String>>(
        mapOf("role" to "assistant", "content" to "Nervesparks")
    ))
        private set

    var message by mutableStateOf("")
        private set

    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch {
            try {
                llm.unload()
            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            }
        }
    }

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llm.load(pathToModel)
                addMessage("assistant", "Loaded $pathToModel")
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                addMessage("error", exc.message ?: "")
            }
        }
    }

    private fun addMessage(role: String, content: String) {
        val newMessage = mapOf("role" to role, "content" to content)

        if (messages.isNotEmpty() && messages.last()["role"] == role) {
            val lastMessageContent = messages.last()["content"] ?: ""
            val updatedContent = "$lastMessageContent$content"
            val updatedLastMessage = messages.last() + ("content" to updatedContent)
            messages = messages.toMutableList().apply {
                set(messages.lastIndex, updatedLastMessage)
            }
        } else {
            messages = messages + listOf(newMessage)
        }
    }
    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }


    fun send() {
        val userMessage = removeExtraWhiteSpaces(message);
        message = ""
        if(userMessage!="" && userMessage!=" ") {
            // Append user's message
            addMessage("user", userMessage)

            val text =
                "system \nYou are a friendly chat-bot who always responds. \n user \n$userMessage \nassistant \n"

            viewModelScope.launch {
                llm.send(text)
                    .catch {
                        Log.e(tag, "send() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect { response ->
                        // Create a new assistant message with the response
                        addMessage("assistant", response)
                    }
            }
        }
    }



    // ... (rest of the functions remain mostly the same)

    fun clear() {
        messages = listOf<Map<String, String>>(
            mapOf("role" to "assistant", "content" to "Nervesparks")
        )
    }

    fun log(message: String) {
        addMessage("log", message)
    }

    fun stop() {
        Llm.instance().stopTextGeneration()
    }
    fun updateMessage(newMessage: String) {
        message = newMessage
    }
}