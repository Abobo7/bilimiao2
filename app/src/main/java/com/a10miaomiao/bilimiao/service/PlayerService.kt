package com.a10miaomiao.bilimiao.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.a10miaomiao.bilimiao.page.setting.VideoSettingFragment
import com.a10miaomiao.bilimiao.service.notification.PlayingNotification
import com.a10miaomiao.bilimiao.widget.player.DanmakuVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer

class PlayerService : Service() {

    companion object {
        val TAG: String = PlayerService::class.java.simpleName
        var selfInstance: PlayerService? = null
            private set

        const val BILIMIAO_PLAYER_PACKAGE_NAME = "com.a10miaomiao.bilimiao.player"
        const val ACTION_CREATED = "$BILIMIAO_PLAYER_PACKAGE_NAME.CREATED"
    }

    var videoPlayerView: DanmakuVideoPlayer? = null
    private var lastNotifiedPlaying: Boolean? = null
    private var lastSessionSyncTimeMs: Long = 0L
    private var lastSessionProgressMs: Long = 0L
    var playerState = 0
        set(value) {
            val prev = field
            field = value
            if (prev != value) {
                updatePlayerState()
            }
        }

    private val playingNotification by lazy { PlayingNotification(this) }

    val mediaSession by lazy {
        val mbrComponent = ComponentName(this, MediaButtonReceiver::class.java)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = mbrComponent
        }
        val mbrPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            mediaButtonIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        MediaSessionCompat(this, "BilimiaoPlayer", mbrComponent, mbrPendingIntent).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(mediaSessionCallback)
        }
    }
//    var mediaSession: MediaSession? = null
    private val info: PlayingInfo = PlayingInfo()
    private var showNotification = true // 是否显示通知栏控制器

    override fun onCreate() {
        super.onCreate()
        selfInstance = this


//        mediaSession?.setCallback(mediaSessionCallback)

        sendBroadcast(Intent(ACTION_CREATED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
                runCatching { MediaButtonReceiver.handleIntent(mediaSession, intent) }
            } else if (intent.action != null) {
                when (intent.action) {
                    PlayingNotification.ACTION_CMD_TOGGLE_PAUSE -> {
                        clickTogglePause()
                    }
                    PlayingNotification.ACTION_CMD_CLOSE -> {
                        closePlayer()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun clickTogglePause() {
        if (playerState == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
            videoPlayerView?.onVideoPause()
        } else {
            videoPlayerView?.onVideoResume()
        }
    }

    private fun closePlayer() {
        videoPlayerView?.release()
        videoPlayerView?.closeVideo()
        clearPlayingInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        selfInstance = null
    }

    fun setPlayingInfo(
        title: String,
        author: String,
        cover: String,
        duration: Long,
    ) {
        info.title = title
        info.author = author
        info.cover = cover
        info.duration = duration
        lastNotifiedPlaying = null
        lastSessionSyncTimeMs = 0L
        lastSessionProgressMs = 0L
        showNotification = getShowNotification()
        if (showNotification) {
            mediaSession.isActive = true
            setupMediaSession()
            playingNotification.setPlayingInfo(info)
        }
    }

    fun clearPlayingInfo() {
        info.title = null
        info.author = null
        info.cover = null
        info.duration = 0L
        lastNotifiedPlaying = null
        mediaSession.isActive = false
        playingNotification.cancel()
    }


    fun getPlayingInfo(): PlayingInfo {
        return info
    }

    fun isPlaying(): Boolean {
        return playerState == GSYVideoPlayer.CURRENT_STATE_PLAYING ||
                playerState == GSYVideoPlayer.CURRENT_STATE_PLAYING_BUFFERING_START
    }

    /**
     * 是否显示通知栏控制器
     */
    private fun getShowNotification(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean(VideoSettingFragment.PLAYER_PLAYING_NOTIFICATION, true)!!
    }

    private fun updatePlayerState() {
        if (!showNotification || info.title == null) {
            return
        }
        val playing = isPlaying()
        val pos = videoPlayerView?.currentPositionWhenPlaying ?: lastSessionProgressMs
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlayingNotification.MEDIA_SESSION_ACTIONS)
            .setState(
                if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                pos,
                if (playing) (videoPlayerView?.speed ?: 1.0f) else 0.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())
        lastSessionSyncTimeMs = android.os.SystemClock.elapsedRealtime()
        lastSessionProgressMs = pos
        if (lastNotifiedPlaying != playing) {
            lastNotifiedPlaying = playing
            playingNotification.updateForPlaying()
        }
    }

    fun setProgress(max: Long, progress: Long) {
        if (showNotification && info.title != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            val playing = isPlaying()
            val speed = if (playing) (videoPlayerView?.speed ?: 1.0f) else 0.0f
            val expectedProgress = lastSessionProgressMs + ((now - lastSessionSyncTimeMs) * speed).toLong()
            val drifted = kotlin.math.abs(progress - expectedProgress) > 3000L
            // 仅在播放状态首次同步或用户拖动进度条(偏差>3s)时更新 MediaSession，
            // 避免每秒跨进程向 MIUI SystemUI / system_server 重复推送 RemoteViews 大图导致系统内存暴涨。
            if (lastNotifiedPlaying != playing || lastSessionSyncTimeMs == 0L || drifted) {
                val stateBuilder = PlaybackStateCompat.Builder()
                    .setActions(PlayingNotification.MEDIA_SESSION_ACTIONS)
                    .setState(
                        if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        progress,
                        speed
                    )
                mediaSession.setPlaybackState(stateBuilder.build())
                lastSessionSyncTimeMs = now
                lastSessionProgressMs = progress
                if (lastNotifiedPlaying != playing) {
                    lastNotifiedPlaying = playing
                    playingNotification.updateForPlaying()
                }
            }
        }
    }

    private fun setupMediaSession() {
        // TODO: 封面
        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, info.author ?: "bilimiao")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, info.author ?: "bilimiao")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.title ?: "bilimiao正在播放")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)
//            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (getPosition() + 1).toLong())

        mediaSession?.setMetadata(metaData.build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPause() {
            super.onPause()
            videoPlayerView?.onVideoPause()
        }

        override fun onPlay() {
            super.onPlay()
            videoPlayerView?.onVideoResume()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            videoPlayerView?.seekTo(pos)
        }
    }

    data class PlayingInfo(
        var title: String? = null,
        var author: String? = null,
        var cover: String? = null,
        var duration: Long = 0L
    )
}