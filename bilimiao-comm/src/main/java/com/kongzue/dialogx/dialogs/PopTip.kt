package com.kongzue.dialogx.dialogs

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

/**
 * Android 4.4.4 (API 19) 原生轻量兼容版 PopTip，
 * 避免原版 DialogX 依赖 API 29 ActivityLifecycleCallbacks.onActivityPreResumed 导致 Dalvik VerifyError。
 */
class PopTip private constructor(
    private val message: CharSequence,
    private val buttonText: CharSequence? = null,
) {
    private var isTop = false
    private var isLong = false
    private var buttonAction: ((PopTip, View) -> Boolean)? = null

    fun setTop(): PopTip {
        isTop = true
        return this
    }

    fun showLong(): PopTip {
        isLong = true
        return this
    }

    fun setButton(listener: (PopTip, View) -> Boolean): PopTip {
        buttonAction = listener
        dispatchShow()
        return this
    }

    internal fun dispatchShow() {
        mainHandler.post {
            val act = currentActivityRef?.get()
            if (!buttonText.isNullOrBlank() && act != null && !act.isFinishing) {
                val rootView = act.findViewById<View>(android.R.id.content)
                if (rootView != null) {
                    val sb = Snackbar.make(
                        rootView,
                        message,
                        if (isLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
                    )
                    sb.setAction(buttonText) { v ->
                        buttonAction?.invoke(this, v)
                    }
                    sb.show()
                    return@post
                }
            }
            val ctx = act ?: appContext ?: return@post
            val toast = Toast.makeText(
                ctx,
                message,
                if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            )
            if (isTop) {
                val yOffset = (80 * ctx.resources.displayMetrics.density).toInt()
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, yOffset)
            }
            toast.show()
        }
    }

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
        private var appContext: Application? = null
        private var currentActivityRef: WeakReference<Activity>? = null

        fun init(application: Application) {
            appContext = application
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    currentActivityRef = WeakReference(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    currentActivityRef = WeakReference(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    currentActivityRef = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivityRef?.get() === activity) {
                        currentActivityRef = null
                    }
                }
            })
        }

        @JvmStatic
        fun show(message: CharSequence): PopTip {
            return PopTip(message, null).also { it.dispatchShow() }
        }

        @JvmStatic
        fun show(message: CharSequence, buttonText: CharSequence): PopTip {
            return PopTip(message, buttonText)
        }
    }
}
