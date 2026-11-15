package com.a10miaomiao.bilimiao.comm.delegate.player.model

import com.a10miaomiao.bilimiao.comm.apis.PlayerAPI
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil

class DashSource(
    val quality: Int,
    val dashData: PlayerAPI.Dash,
    val uposHost: String = "",
    val preferredCodec: String = "avc",
) {

    private fun matchesCodec(item: PlayerAPI.DashItem, codec: String): Boolean {
        val c = item.codecs.lowercase()
        return when (codec.lowercase()) {
            "avc" -> c.startsWith("avc")
            "hevc" -> c.startsWith("hev") || c.startsWith("hvc")
            "av1" -> c.startsWith("av01")
            else -> c.startsWith("avc")
        }
    }

    fun getDashVideo(): PlayerAPI.DashItem? {
        val videoList = dashData.video
        if (videoList.isEmpty()) return null
        // 1. 优先匹配指定清晰度 + 首选编码（默认强制 AVC/H.264，防止 Android 4.4 老设备硬解 HEVC/AV1 绿屏或黑屏）
        videoList.find { it.id == quality && matchesCodec(it, preferredCodec) }?.let { return it }
        // 2. 指定清晰度下无首选编码时，尝试回退至指定清晰度的 AVC
        videoList.find { it.id == quality && matchesCodec(it, "avc") }?.let { return it }
        // 3. 指定清晰度任意流
        videoList.find { it.id == quality }?.let { return it }
        // 4. 任意清晰度下的首选编码或 AVC
        videoList.find { matchesCodec(it, preferredCodec) }?.let { return it }
        videoList.find { matchesCodec(it, "avc") }?.let { return it }
        return videoList.last()
    }

    fun getDashAudio(): PlayerAPI.DashItem? {
        val audioList = dashData.audio
        if (audioList.isNotEmpty()) {
            return audioList[0]
        }
        return null
    }

    fun getMDPUrl(
        video: PlayerAPI.DashItem = getDashVideo()!!
    ): String {
        val audio = getDashAudio()
        val mpdStr = """
<MPD xmlns="urn:mpeg:DASH:schema:MPD:2011" profiles="urn:mpeg:dash:profile:isoff-on-demand:2011" type="static" mediaPresentationDuration="PT${dashData.duration}S" minBufferTime="PT${dashData.min_buffer_time}S">
    <Period start="PT0S">
        <AdaptationSet>
            <ContentComponent contentType="video" id="1" />
            <Representation bandwidth="${video.bandwidth}" codecs="${video.codecs}" height="${video.height}" id="${video.id}" mimeType="${video.mime_type}" width="${video.width}">
                <BaseURL></BaseURL>
                <SegmentBase indexRange="${video.segment_base.index_range}">
                    <Initialization range="${video.segment_base.initialization}" />
                </SegmentBase>
            </Representation>
        </AdaptationSet>
        ${
            if (audio != null) {
                var audioUrl = audio.base_url
                if (uposHost.isNotBlank()) {
                    audioUrl = UrlUtil.replaceHost(audioUrl, uposHost)
                }
                """
                 <AdaptationSet>
                    <ContentComponent contentType="audio" id="2" />
                    <Representation bandwidth="${audio.bandwidth}" codecs="${audio.codecs}" id="${audio.id}" mimeType="${audio.mime_type}" >
                        <BaseURL>${audioUrl.replace("&", "&amp;")}</BaseURL>
                        <SegmentBase indexRange="${audio.segment_base.index_range}">
                            <Initialization range="${audio.segment_base.initialization}" />
                        </SegmentBase>
                    </Representation>
                </AdaptationSet>
                """.trimIndent()
            } else {
                ""
            }
        }
    </Period>
</MPD>
        """.trimIndent()
        var url = video.base_url
        if (uposHost.isNotBlank()) {
            url = UrlUtil.replaceHost(url, uposHost)
        }
        return "[dash-mpd]\n" + url + "\n" + mpdStr.replace("\n", "")
    }
}
