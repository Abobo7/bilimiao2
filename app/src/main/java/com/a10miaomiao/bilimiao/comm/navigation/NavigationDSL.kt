package com.a10miaomiao.bilimiao.comm.navigation

import android.net.Uri
import androidx.navigation.NavController
import com.a10miaomiao.bilimiao.page.search.SearchStartFragment
import com.a10miaomiao.bilimiao.widget.comm.ScaffoldView

fun NavController.navigateToCompose(url: String) {
    navigate(Uri.parse(url))
}

fun ScaffoldView.openSearchDrawer() {
    openDrawer()
    (drawerFragment as? SearchStartFragment)?.showSoftInput()
}

fun ScaffoldView.closeSearchDrawer() {
    (drawerFragment as? SearchStartFragment)?.hideSoftInput()
    closeDrawer()
}
