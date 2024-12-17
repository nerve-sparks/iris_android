package com.nervesparks.iris.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import com.nervesparks.iris.MainViewModel
import kotlinx.coroutines.launch


@Composable
fun ScrollToBottomButton(
    viewModel: MainViewModel,
    scrollState: LazyListState,
    messages: List<Any>
) {
    val coroutineScope = rememberCoroutineScope()

    // State to track if auto-scrolling is enabled
    var isAutoScrolling by remember { mutableStateOf(false) }

    // State to control the button's visibility
    var isButtonVisible by remember { mutableStateOf(true) }

    // Determine if the user can scroll down
    val canScrollDown by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

    // Continuously scroll to the bottom while auto-scrolling is enabled
    LaunchedEffect(viewModel.messages.size, isAutoScrolling) {
        if (isAutoScrolling) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size + 1)
            }
        }
    }

    // Stop auto-scrolling when the user scrolls manually
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isAutoScrolling = false
            isButtonVisible = true // Show the button again if the user scrolls manually
        }
    }

    // Continuously monitor changes in the last item's content
    LaunchedEffect(messages.lastOrNull()) {
        if (isAutoScrolling && messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size + 1)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = (canScrollDown || isAutoScrolling) && isButtonVisible, // Show button if needed
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = {
                    isAutoScrolling = true // Enable auto-scrolling
                    isButtonVisible = false // Hide the button on click
                    coroutineScope.launch {
                        scrollState.scrollToItem(viewModel.messages.size + 1)
                    }
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp),
                // Ensures a circular shape
                shape = RoundedCornerShape(percent = 50),
                containerColor = Color.White.copy(alpha = 0.5f),
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = Color.White // White icon for better visibility
                )
            }
        }
    }
}