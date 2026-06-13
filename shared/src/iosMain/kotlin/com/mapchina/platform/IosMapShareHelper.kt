package com.mapchina.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIGraphicsImageRenderer

@OptIn(ExperimentalForeignApi::class)
class IosMapShareHelper : MapShareHelper {

    private var targetView: UIView? = null

    fun setTargetView(view: UIView) {
        targetView = view
    }

    override fun captureAndShare() {
        val view = targetView ?: return
        val renderer = UIGraphicsImageRenderer(view.bounds)
        val image = renderer.imageWithActions { context ->
            view.layer.renderInContext(context!!.CGContext)
        }
        val activityVC = UIActivityViewController(listOf(image as Any), null)
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }
}
