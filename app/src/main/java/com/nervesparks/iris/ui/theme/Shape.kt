package com.nervesparks.iris.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Samsung Galaxy S24 Ultra optimized shapes
// Following Samsung One UI design language with modern Material 3
val SamsungShapes = Shapes(
    // Extra small components - chips, buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small components - cards, dialogs
    small = RoundedCornerShape(8.dp),
    
    // Medium components - sheets, larger cards
    medium = RoundedCornerShape(12.dp),
    
    // Large components - app bars, navigation drawers
    large = RoundedCornerShape(16.dp),
    
    // Extra large components - bottom sheets, full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for specific Samsung Galaxy S24 Ultra optimizations
val ChatBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,
    bottomEnd = 18.dp
)

val UserChatBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp
)

val BottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val FloatingActionButtonShape = RoundedCornerShape(16.dp)

val CardShape = RoundedCornerShape(16.dp)

val ButtonShape = RoundedCornerShape(12.dp)

val TextFieldShape = RoundedCornerShape(12.dp)