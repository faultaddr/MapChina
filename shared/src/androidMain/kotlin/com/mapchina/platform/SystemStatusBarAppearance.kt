package com.mapchina.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SystemStatusBarAppearance(darkIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity()

    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = darkIcons
        }
    }
    DisposableEffect(activity, view) {
        onDispose {
            activity?.let {
                WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = true
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
