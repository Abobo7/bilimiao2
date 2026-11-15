package com.a10miaomiao.bilimiao.config

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import splitties.views.backgroundColor
import splitties.views.padding

object ViewStyle {
    val block
        get() = { v: View ->
            v.run {
                backgroundColor = config.blockBackgroundColor
                padding = config.dividerSize
            }
        }

    val circle
        get() = { v: View ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Api21OutlineHelper.applyCircle(v)
            }
        }

    fun roundRect(roundCorner: Int) = { v: View ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21OutlineHelper.applyRoundRect(v, roundCorner)
        }
    }

    fun card(roundCorner: Int, elevation: Float) = { v: View ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21OutlineHelper.applyCard(v, roundCorner, elevation)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private object Api21OutlineHelper {
        fun applyCircle(v: View) {
            v.clipToOutline = true
            v.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val left = 0
                    val top = (view.height - view.width) / 2
                    val right = view.width
                    val bottom = (view.height - view.width) / 2 + view.width
                    outline.setOval(left, top, right, bottom)
                }
            }
        }

        fun applyRoundRect(v: View, roundCorner: Int) {
            v.clipToOutline = true
            v.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, roundCorner.toFloat())
                }
            }
        }

        fun applyCard(v: View, roundCorner: Int, elevation: Float) {
            v.clipToOutline = true
            v.elevation = elevation
            v.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, roundCorner.toFloat())
                }
            }
        }
    }
}
