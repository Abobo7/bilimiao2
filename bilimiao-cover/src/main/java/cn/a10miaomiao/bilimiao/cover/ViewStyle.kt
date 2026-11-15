package cn.a10miaomiao.bilimiao.cover

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi

object ViewStyle {

    fun roundRect(roundCorner: Int) = { v: View ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21OutlineHelper.applyRoundRect(v, roundCorner)
        }
    }

    fun topRoundRect(roundCorner: Int) = { v: View ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21OutlineHelper.applyTopRoundRect(v, roundCorner)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private object Api21OutlineHelper {
        fun applyRoundRect(v: View, roundCorner: Int) {
            v.clipToOutline = true
            v.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, roundCorner.toFloat())
                }
            }
        }

        fun applyTopRoundRect(v: View, roundCorner: Int) {
            v.clipToOutline = true
            v.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height + roundCorner, roundCorner.toFloat())
                }
            }
        }
    }
}
