package com.mapchina.platform

import android.view.HapticFeedbackConstants
import android.view.View

class AndroidHapticFeedback(private val view: View) : HapticFeedback {
    override fun perform(type: HapticType) {
        val constant = when (type) {
            HapticType.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            HapticType.MEDIUM -> HapticFeedbackConstants.CONFIRM
            HapticType.HEAVY -> HapticFeedbackConstants.CONFIRM
            HapticType.SELECTION -> HapticFeedbackConstants.CLOCK_TICK
            HapticType.SUCCESS -> HapticFeedbackConstants.CONFIRM
            HapticType.WARNING -> HapticFeedbackConstants.REJECT
            HapticType.ERROR -> HapticFeedbackConstants.REJECT
        }
        view.performHapticFeedback(constant)
    }
}
