package com.example.llama

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llm: Llm = Llm.instance()) : ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(
        listOf<Map<String, String>>(

        )
    )
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
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""
        if (userMessage.isNotBlank()) {
            addMessage("user", userMessage)
            val builder = StringBuilder()

            // Append initial system message
            builder.append("system\nYou are a friendly and precise chat-bot named Iris, who always responds in brief responses.\n")

            // Append all previous messages
            for (i in 1 until messages.size) { // Start iterating from the second message
                val msg = messages[i]
                val role = msg["role"]
                val content = msg["content"]
                if (role != null && content != null) {
                    builder.append("$role\n$content\n")
                }
            }

            // Append the new user message
            builder.append("user\n$userMessage\n")

            val text = builder.toString()

            viewModelScope.launch {
                llm.send(text)
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


    // ... (rest of the functions remain mostly the same)

    fun clear() {
        messages = listOf<Map<String, String>>(

        )
    }

    fun log(message: String) {
        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llm.getIsSending()
    }

    fun getIsMarked(): Boolean {
        return llm.getIsMarked()
    }

    fun stop() {
        Llm.instance().stopTextGeneration()
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

}