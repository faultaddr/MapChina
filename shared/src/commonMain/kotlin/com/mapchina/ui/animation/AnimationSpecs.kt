package com.mapchina.ui.animation

import androidx.compose.animation.core.*

object AnimationSpecs {
    // ===== Spring curves =====
    val springGentle = spring<Float>(dampingRatio = 0.7f, stiffness = 150f)
    val springFluid = spring<Float>(dampingRatio = 0.85f, stiffness = 90f)
    val springBouncy = spring<Float>(dampingRatio = 0.6f, stiffness = 200f)
    val springHeavy = spring<Float>(dampingRatio = 0.4f, stiffness = 300f)
    val springSnap = spring<Float>(dampingRatio = 0.3f, stiffness = 500f)

    // ===== Tween curves =====
    val tweenSlowEase = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
    val tweenRipple = tween<Float>(durationMillis = 600, easing = LinearOutSlowInEasing)
    val tweenQuick = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
    val tweenProgress = tween<Float>(durationMillis = 600, easing = FastOutSlowInEasing)
    val tweenCoverage = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)

    // ===== Timing constants =====
    object Duration {
        val buttonPress = 80
        val buttonSpringBack = 200
        val statusChange = 200
        val pageTransition = 350
        val mapDrillDown = 500
        val rippleExpand = 600
        val unlockOverlay = 200
        val unlockBurst = 300
        val unlockPopIn = 300
        val unlockContent = 400
        val levelUpScreenFlash = 100
        val toastShow = 300
        val toastHold = 2000
        val toastHide = 250
        val tabTransition = 200
        val detailPop = 250
        val panelSlide = 300
        val chevronRotation = 300
        val glowPulse = 1000
        val entranceFade = 400
    }

    object Stagger {
        val listItem = 60
        val gridItem = 50
        val rowItem = 80
        val chartSection = 150
        val numberCard = 100
    }

    object Scale {
        val buttonPress = 0.97f
        val cardPress = 0.98f
        val drillDownZoom = 1.05f
        val unlockOverShoot = 1.05f
        val popInFrom = 0.8f
        val entranceFrom = 0.95f
        val tabBouncePeak = 1.3f
        val levelBouncePeak = 1.2f
        val burstExpand = 3f
    }

    // ===== Additional spring curves =====
    val springTabBounce = spring<Float>(dampingRatio = 0.5f, stiffness = 400f)
    val springPressSnap = spring<Float>(dampingRatio = 0.5f, stiffness = 500f)
    val springUnlockBounce = spring<Float>(dampingRatio = 0.6f, stiffness = 200f)
    val springUnlockSettle = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
    val springLevelBounce = spring<Float>(dampingRatio = 0.4f, stiffness = 300f)

    // ===== Easing curves =====
    val easingStandard = FastOutSlowInEasing
    val easingDecelerate = LinearOutSlowInEasing
}
