package com.mapchina.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

class IosHapticFeedback : HapticFeedback {
    private val lightGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavyGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val selectionGenerator = UISelectionFeedbackGenerator()
    private val notificationGenerator = UINotificationFeedbackGenerator()

    init {
        lightGenerator.prepare()
        mediumGenerator.prepare()
        heavyGenerator.prepare()
        selectionGenerator.prepare()
        notificationGenerator.prepare()
    }

    override fun perform(type: HapticType) {
        when (type) {
            HapticType.LIGHT -> lightGenerator.impactOccurred()
            HapticType.MEDIUM -> mediumGenerator.impactOccurred()
            HapticType.HEAVY -> heavyGenerator.impactOccurred()
            HapticType.SELECTION -> selectionGenerator.selectionChanged()
            HapticType.SUCCESS -> notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            HapticType.WARNING -> notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            HapticType.ERROR -> notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
        }
    }
}
