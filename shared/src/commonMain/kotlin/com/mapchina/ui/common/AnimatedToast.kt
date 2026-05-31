package com.mapchina.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapchina.ui.animation.AnimationSpecs
import kotlinx.coroutines.delay

enum class ToastType { Info, Success, Error }

@Composable
fun AnimatedToast(
    message: String,
    type: ToastType = ToastType.Info,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(AnimationSpecs.Duration.toastHold.toLong())
        visible = false
        delay(AnimationSpecs.Duration.toastHide.toLong())
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -50 } + fadeIn(tween(AnimationSpecs.Duration.toastShow)),
        exit = slideOutVertically { -50 } + fadeOut(tween(AnimationSpecs.Duration.toastHide))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333333))
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(if (type == ToastType.Error) Color(0xFFE94560) else Color(0xFFFFD700))
            )
            Spacer(Modifier.width(12.dp))
            Text(message, color = Color.White)
        }
    }
}
