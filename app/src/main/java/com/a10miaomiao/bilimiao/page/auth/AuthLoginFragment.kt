package com.a10miaomiao.bilimiao.page.auth

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import androidx.navigation.fragment.findNavController
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.connectUi
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.QRLoginInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.WebKeyInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.lazyUiDi
import com.a10miaomiao.bilimiao.comm.miaoBindingUi
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.navigation.FragmentNavigatorBuilder
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip

class AuthLoginFragment : Fragment(), DIAware, MyPage {

    companion object : FragmentNavigatorBuilder() {
        override val name = "auth.login"
        override fun FragmentNavigatorDestinationBuilder.init() {
            deepLink("bilimiao://auth/login")
            deepLink("bilimiao://auth/qr_login")
        }
    }

    override val pageConfig = myPageConfig {
        title = "登录哔哩哔哩"
    }

    override val di: DI by lazyUiDi(ui = { dummyUi })
    private val windowStore by instance<WindowStore>()
    private val userStore by instance<UserStore>()

    private val dummyUi = miaoBindingUi { View(ctx) }

    private var rootScroll: ScrollView? = null
    private var qrImageView: ImageView? = null
    private var qrProgress: ProgressBar? = null
    private var statusTv: TextView? = null
    private var refreshQrBtn: Button? = null
    private var usernameEt: EditText? = null
    private var passwordEt: EditText? = null
    private var pwdLoginBtn: Button? = null

    private var pollJob: Job? = null
    private var currentAuthCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val cfg = ctx.config

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(cfg.windowBackgroundColor)
        }
        rootScroll = scroll

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = ctx.dip(16)
            setPadding(pad, ctx.dip(28), pad, ctx.dip(72))
        }

        val qrCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(cfg.blockBackgroundColor)
            val p = ctx.dip(16)
            setPadding(p, p, p, p)
        }

        val qrTitle = TextView(ctx).apply {
            text = "方式一：客户端扫码登录（推荐）"
            textSize = 16f
            setTextColor(cfg.foregroundColor)
            gravity = Gravity.CENTER
        }
        qrCard.addView(qrTitle)

        val qrHint = TextView(ctx).apply {
            text = "请打开手机哔哩哔哩客户端「扫一扫」扫描下方二维码"
            textSize = 13f
            setTextColor(cfg.foregroundAlpha45Color)
            gravity = Gravity.CENTER
            setPadding(0, ctx.dip(6), 0, ctx.dip(12))
        }
        qrCard.addView(qrHint)

        val progress = ProgressBar(ctx)
        qrProgress = progress
        qrCard.addView(progress, LinearLayout.LayoutParams(ctx.dip(42), ctx.dip(42)).apply {
            gravity = Gravity.CENTER
        })

        val qrIv = ImageView(ctx).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            val p = ctx.dip(8)
            setPadding(p, p, p, p)
            setOnClickListener { loadQrCode() }
        }
        qrImageView = qrIv
        qrCard.addView(qrIv, LinearLayout.LayoutParams(ctx.dip(220), ctx.dip(220)).apply {
            gravity = Gravity.CENTER
        })

        val stTv = TextView(ctx).apply {
            text = "正在获取登录二维码..."
            textSize = 14f
            setTextColor(cfg.themeColor)
            gravity = Gravity.CENTER
            setPadding(0, ctx.dip(10), 0, ctx.dip(8))
        }
        statusTv = stTv
        qrCard.addView(stTv)

        val refreshBtn = Button(ctx).apply {
            text = "刷新二维码"
            setOnClickListener { loadQrCode() }
        }
        refreshQrBtn = refreshBtn
        qrCard.addView(refreshBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        content.addView(qrCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // 方式二：账号密码登录 + Cookie 导入
        val pwdCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cfg.blockBackgroundColor)
            val p = ctx.dip(16)
            setPadding(p, p, p, p)
        }

        val pwdTitle = TextView(ctx).apply {
            text = "方式二：账号密码 / Cookie 登录"
            textSize = 16f
            setTextColor(cfg.foregroundColor)
        }
        pwdCard.addView(pwdTitle)

        val userField = EditText(ctx).apply {
            hint = "手机号 / 邮箱 / 用户名"
            setSingleLine(true)
            setTextColor(cfg.foregroundColor)
            setHintTextColor(cfg.foregroundAlpha45Color)
        }
        usernameEt = userField
        pwdCard.addView(userField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(8) })

        val passField = EditText(ctx).apply {
            hint = "登录密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setTextColor(cfg.foregroundColor)
            setHintTextColor(cfg.foregroundAlpha45Color)
        }
        passwordEt = passField
        pwdCard.addView(passField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(6) })

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cookieBtn = Button(ctx).apply {
            text = "粘贴 Cookie 登录"
            setOnClickListener { showCookieLoginDialog() }
        }
        btnRow.addView(cookieBtn)

        val loginBtn = Button(ctx).apply {
            text = "密码登录"
            setOnClickListener { performPasswordLogin() }
        }
        pwdLoginBtn = loginBtn
        btnRow.addView(loginBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = ctx.dip(8) })

        pwdCard.addView(btnRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(10) })

        content.addView(pwdCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(14) })

        scroll.addView(content)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            windowStore.stateFlow.collect { state ->
                val insets = state.windowInsets
                val pad = requireContext().dip(16)
                (rootScroll?.getChildAt(0) as? LinearLayout)?.setPadding(
                    pad + insets.left,
                    insets.top + requireContext().dip(16),
                    pad + insets.right,
                    insets.bottom + requireContext().dip(76)
                )
            }
        }
        loadQrCode()
    }

    override fun onDestroyView() {
        pollJob?.cancel()
        super.onDestroyView()
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }

    private fun loadQrCode() {
        pollJob?.cancel()
        qrProgress?.visibility = View.VISIBLE
        qrImageView?.visibility = View.GONE
        statusTv?.text = "正在获取登录二维码..."

        pollJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = BiliApiService.authApi
                    .qrCode()
                    .awaitCall()
                    .gson<ResultInfo<QRLoginInfo>>()
                if (!res.isSuccess) {
                    withContext(Dispatchers.Main) {
                        qrProgress?.visibility = View.GONE
                        statusTv?.text = "获取二维码失败：${res.message}"
                    }
                    return@launch
                }
                val qrSize = requireContext().dip(204)
                val bmp = generateQrBitmap(res.data.url, qrSize)
                val authCode = res.data.auth_code
                currentAuthCode = authCode
                withContext(Dispatchers.Main) {
                    qrProgress?.visibility = View.GONE
                    qrImageView?.setImageBitmap(bmp)
                    qrImageView?.visibility = View.VISIBLE
                    statusTv?.text = "等待扫码..."
                }
                pollQrStatus(authCode)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    qrProgress?.visibility = View.GONE
                    statusTv?.text = "网络异常：${e.message ?: e}"
                }
            }
        }
    }

    private suspend fun pollQrStatus(authCode: String) {
        while (currentAuthCode == authCode) {
            delay(2500)
            try {
                val res = BiliApiService.authApi
                    .checkQrCode(authCode)
                    .awaitCall()
                    .gson<ResultInfo<LoginInfo.QrLoginInfo>>()
                when (res.code) {
                    86039 -> {
                        // 未扫码
                    }
                    86090 -> {
                        withContext(Dispatchers.Main) {
                            statusTv?.text = "已扫码，请在手机端点击「确认登录」"
                        }
                    }
                    86038, -3 -> {
                        withContext(Dispatchers.Main) {
                            statusTv?.text = "二维码已过期，请点击「刷新二维码」"
                        }
                        return
                    }
                    0 -> {
                        val loginInfo = res.data.toLoginInfo()
                        BilimiaoCommApp.commApp.saveAuthInfo(loginInfo)
                        fetchUserAccountAndFinish()
                        return
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            statusTv?.text = "登录失败(${res.code})：${res.message}"
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performPasswordLogin() {
        val user = usernameEt?.text?.toString()?.trim().orEmpty()
        val pass = passwordEt?.text?.toString().orEmpty()
        if (user.isBlank() || pass.isBlank()) {
            Toast.makeText(requireContext(), "请输入账号和密码", Toast.LENGTH_SHORT).show()
            return
        }
        pwdLoginBtn?.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val keyRes = BiliApiService.authApi.webKey().awaitCall().gson<ResultInfo<WebKeyInfo>>()
                if (!keyRes.isSuccess) {
                    throw Exception(keyRes.message)
                }
                val loginRes = BiliApiService.authApi.oauth2Login(
                    username = user,
                    passport = pass,
                    key = keyRes.data.key,
                    rhash = keyRes.data.hash
                ).awaitCall().gson<ResultInfo<LoginInfo>>()
                if (loginRes.isSuccess && loginRes.data.token_info != null) {
                    BilimiaoCommApp.commApp.saveAuthInfo(loginRes.data)
                    fetchUserAccountAndFinish()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "密码登录需人机验证或返回(${loginRes.code}: ${loginRes.message})，请使用上方扫码或粘贴 Cookie 登录",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "登录出错：${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    pwdLoginBtn?.isEnabled = true
                }
            }
        }
    }

    private fun showCookieLoginDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = "粘贴包含 SESSDATA=...; bili_jct=... 的完整 Cookie"
            minLines = 3
        }
        AlertDialog.Builder(ctx)
            .setTitle("粘贴 Cookie 登录")
            .setView(input)
            .setPositiveButton("验证并登录") { _, _ ->
                val raw = input.text?.toString()?.trim().orEmpty()
                if (raw.isBlank()) return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        BilimiaoCommApp.commApp.setRawCookieString(raw)
                        fetchUserAccountAndFinish()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(ctx, "Cookie 验证失败：${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private suspend fun fetchUserAccountAndFinish() {
        val res = BiliApiService.authApi
            .account()
            .awaitCall()
            .gson<ResultInfo<UserInfo>>()
        if (res.isSuccess) {
            withContext(Dispatchers.Main) {
                userStore.setUserInfo(res.data)
                Toast.makeText(requireContext(), "欢迎回来，${res.data.name}！", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            throw Exception(res.message)
        }
    }
}
