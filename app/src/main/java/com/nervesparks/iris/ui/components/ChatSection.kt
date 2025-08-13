package com.nervesparks.iris.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.PerformanceMonitor
import com.nervesparks.iris.ui.components.ThinkingMessage
import com.nervesparks.iris.llm.ReasoningParser
import com.nervesparks.iris.ui.theme.ComponentStyles


@Composable
fun ChatMessageList(viewModel: MainViewModel, scrollState: LazyListState) {
    val messages = viewModel.messages
    val context = LocalContext.current

    LazyColumn(state = scrollState) {
        itemsIndexed(messages.drop(3), key = { idx, messageMap ->
            // Prefer unique message id if present; else fall back to index
            (messageMap["id"] ?: idx).hashCode()
        }) { idx, messageMap ->
            val role = messageMap["role"] ?: ""
            val content = (messageMap["content"] ?: "").trimEnd()
            if (role != "system") {
                when (role) {
                    "codeBlock" -> CodeBlockMessage(content)
                    "assistant" -> {
                        val (reasoningContent, _) = ReasoningParser.parse(content, viewModel.supportsReasoning)
                        if (viewModel.supportsReasoning && reasoningContent.isNotEmpty()) {
                            ThinkingMessage(
                                message = content,
                                viewModel = viewModel,
                                showThinkingTokens = viewModel.showThinkingTokens,
                                onLongClick = {
                                    if (viewModel.getIsSending()) {
                                        Toast.makeText(
                                            context,
                                            "Wait till generation is done!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        viewModel.toggler = true
                                    }
                                }
                            )
                        } else {
                            val prevRole = messages.getOrNull(idx + 2)?.get("role")
                            val nextRole = messages.getOrNull(idx + 4)?.get("role")
                            val groupedPrev = prevRole == role
                            val groupedNext = nextRole == role
                            MessageBubble(
                                message = content,
                                isUser = false,
                                isGroupedPrev = groupedPrev,
                                isGroupedNext = groupedNext,
                                onLongClick = {
                                    if (viewModel.getIsSending()) {
                                        Toast.makeText(
                                            context,
                                            "Wait till generation is done!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        viewModel.toggler = true
                                    }
                                }
                            )
                        }
                    }
                    else -> {
                        val prevRole = messages.getOrNull(idx + 2)?.get("role")
                        val nextRole = messages.getOrNull(idx + 4)?.get("role")
                        val groupedPrev = prevRole == role
                        val groupedNext = nextRole == role
                        MessageBubble(
                            message = content,
                            isUser = true,
                            isGroupedPrev = groupedPrev,
                            isGroupedNext = groupedNext,
                            onLongClick = {
                            if (viewModel.getIsSending()) {
                                Toast.makeText(
                                    context,
                                    "Wait till generation is done!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.toggler = true
                            }
                        }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(ComponentStyles.smallPadding).fillMaxWidth()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserOrAssistantMessage(role: String, message: String, onLongClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(ComponentStyles.smallPadding)
    ) {
        if (role == "assistant") MessageIcon(iconRes = R.drawable.logo, description = "Bot Icon")

        Box(
            modifier = Modifier
                .padding(horizontal = ComponentStyles.smallPadding)
                .background(
                    color = if (role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = ComponentStyles.pillShape
                )
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {}
                )
                .padding(ComponentStyles.defaultPadding)
        ) {
            Text(
                text = message.removePrefix("```"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.removePrefix("```")))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(ComponentStyles.defaultIconSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Copy message",
                    tint = if (role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (role == "user") MessageIcon(iconRes = R.drawable.user_icon, description = "User Icon")
    }
}

@Composable
private fun CodeBlockMessage(content: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .padding(horizontal = ComponentStyles.defaultSpacing, vertical = ComponentStyles.smallPadding)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = ComponentStyles.smallCardShape)
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(content.removePrefix("```")))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.align(Alignment.TopEnd).size(ComponentStyles.defaultIconSize)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Copy code",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = content.removePrefix("```"),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        )
    }
}

@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(ComponentStyles.defaultIconSize)
    )
}

