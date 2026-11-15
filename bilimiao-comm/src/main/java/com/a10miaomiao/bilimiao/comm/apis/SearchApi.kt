package com.a10miaomiao.bilimiao.comm.apis

import android.net.Uri
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp

class SearchApi {

    /**
     * 关键字联想列表
     */
    fun suggestList(keyword: String) = MiaoHttp.request {
        url = "https://s.search.bilibili.com/main/suggest?suggest_type=accurate&sub_type=tag&main_ver=v1&term=${Uri.encode(keyword)}"
    }

    /**
     * 综合视频搜索（签名接口）
     */
    fun searchArchive(
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        order: String,
        duration: Int,
        rid: Int,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApp(
            "x/v2/search",
            "duration" to duration.toString(),
            "order" to order,
            "rid" to rid.toString(),
            "keyword" to keyword,
            "pn" to pageNum.toString(),
            "ps" to pageSize.toString(),
        )
    }

    /**
     * 番剧搜索（签名接口，type=7）
     */
    fun searchBangumi(
        keyword: String,
        pageNum: Int,
        pageSize: Int
    ) = MiaoHttp.request {
        url = BiliApiService.biliApp(
            "x/v2/search/type",
            "keyword" to keyword,
            "type" to "7",
            "pn" to pageNum.toString(),
            "ps" to pageSize.toString(),
        )
    }

    /**
     * UP主搜索（签名接口，type=2）
     */
    fun searchUpper(
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        userType: Int = 0,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApp(
            "x/v2/search/type",
            "disable_rcmd" to "0",
            "highlight" to "1",
            "order" to "totalrank",
            "keyword" to keyword,
            "type" to "2",
            "user_type" to userType.toString(),
            "pn" to pageNum.toString(),
            "ps" to pageSize.toString(),
        )
    }
}
