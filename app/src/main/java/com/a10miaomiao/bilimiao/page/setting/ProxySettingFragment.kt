package com.a10miaomiao.bilimiao.page.setting

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import androidx.navigation.fragment.findNavController
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.lazyUiDi
import com.a10miaomiao.bilimiao.comm.miaoBindingUi
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.navigation.FragmentNavigatorBuilder
import com.a10miaomiao.bilimiao.comm.navigation.MainNavArgs
import com.a10miaomiao.bilimiao.comm.proxy.BiliUposInfo
import com.a10miaomiao.bilimiao.comm.proxy.ProxyHelper
import com.a10miaomiao.bilimiao.comm.proxy.ProxyServerInfo
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip

class ProxySettingFragment : Fragment(), DIAware, MyPage {

    companion object : FragmentNavigatorBuilder() {
        override val name = "setting.proxy"
        override fun FragmentNavigatorDestinationBuilder.init() {
            deepLink("bilimiao://setting/proxy")
            deepLink("bilimiao://setting/proxy/select")
            argument(MainNavArgs.type) {
                defaultValue = "manage"
            }
        }

        fun createArguments(mode: String = "manage"): Bundle = bundleOf(
            MainNavArgs.type to mode
        )

        val UPOS_LIST = listOf(
            BiliUposInfo("none", "不替换（默认）", ""),
            BiliUposInfo("ali", "ali（阿里云）", "upos-sz-mirrorali.bilivideo.com"),
            BiliUposInfo("cos", "cos（腾讯云）", "upos-sz-mirrorcos.bilivideo.com"),
            BiliUposInfo("hw", "hw（华为云）", "upos-sz-mirrorhw.bilivideo.com"),
            BiliUposInfo("akamai", "akamai（Akamai海外）", "upos-hz-mirrorakam.akamaized.net"),
            BiliUposInfo("aliov", "aliov（阿里海外）", "upos-sz-mirroraliov.bilivideo.com"),
            BiliUposInfo("cosov", "cosov（腾讯海外）", "upos-sz-mirrorcosov.bilivideo.com"),
            BiliUposInfo("tf_hw", "tf_hw（华为）", "upos-tf-all-hw.bilivideo.com"),
            BiliUposInfo("tf_tx", "tf_tx（腾讯）", "upos-tf-all-tx.bilivideo.com"),
        )
    }

    override val pageConfig = myPageConfig {
        title = "区域限制与 UPOS 节点设置"
    }

    override val di: DI by lazyUiDi(ui = { dummyUi })
    private val windowStore by instance<WindowStore>()
    private val basePlayerDelegate by instance<BasePlayerDelegate>()

    private val dummyUi = miaoBindingUi { View(ctx) }

    private var serverContainer: LinearLayout? = null
    private var rootScroll: ScrollView? = null
    private val isSelectMode: Boolean
        get() = arguments?.getString(MainNavArgs.type) == "select"

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
            val p = ctx.dip(16)
            setPadding(p, ctx.dip(24), p, ctx.dip(72))
        }

        // UPOS CDN 选择卡片
        val uposCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cfg.blockBackgroundColor)
            val p = ctx.dip(14)
            setPadding(p, p, p, p)
        }
        uposCard.addView(TextView(ctx).apply {
            text = "UPOS 视频 CDN 节点替换"
            textSize = 15f
            setTextColor(cfg.foregroundColor)
        })
        val currentUpos = ProxyHelper.uposName(ctx)
        val spinner = Spinner(ctx).apply {
            adapter = ArrayAdapter<String>(
                ctx,
                android.R.layout.simple_spinner_item,
                UPOS_LIST.map { it.label }
            ).also { ad ->
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            val idx = UPOS_LIST.indexOfFirst { it.name == currentUpos }.coerceAtLeast(0)
            setSelection(idx)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    ProxyHelper.saveUposName(ctx, UPOS_LIST[position].name)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        uposCard.addView(spinner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(8) })
        content.addView(uposCard)

        // 代理服务器列表标题栏
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(ctx).apply {
            text = if (isSelectMode) "点击下方代理服务器立即重试播放" else "自定义区域限制代理服务器"
            textSize = 15f
            setTextColor(cfg.foregroundColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(Button(ctx).apply {
            text = "添加服务器"
            setOnClickListener { showEditDialog(-1, null) }
        })
        content.addView(headerRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(14) })

        val listLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        serverContainer = listLayout
        content.addView(listLayout, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dip(8) })

        scroll.addView(content)
        refreshServerList()
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            windowStore.stateFlow.collect { state ->
                val insets = if (isSelectMode) state.bottomSheetContentInsets else state.windowInsets
                val pad = requireContext().dip(16)
                (rootScroll?.getChildAt(0) as? LinearLayout)?.setPadding(
                    pad + insets.left,
                    insets.top + requireContext().dip(12),
                    pad + insets.right,
                    insets.bottom + requireContext().dip(72)
                )
            }
        }
    }

    private fun refreshServerList() {
        val container = serverContainer ?: return
        val ctx = requireContext()
        val cfg = ctx.config
        container.removeAllViews()
        val list = ProxyHelper.serverList(ctx)
        if (list.isEmpty()) {
            container.addView(TextView(ctx).apply {
                text = "暂未添加自定义代理服务器"
                setTextColor(cfg.foregroundAlpha45Color)
                gravity = Gravity.CENTER
                setPadding(0, ctx.dip(32), 0, ctx.dip(32))
            })
            return
        }
        list.forEachIndexed { index, item ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(cfg.blockBackgroundColor)
                val p = ctx.dip(12)
                setPadding(p, p, p, p)
                setOnClickListener {
                    if (isSelectMode) {
                        val uposHost = UPOS_LIST.find { u -> u.name == ProxyHelper.uposName(ctx) }?.host.orEmpty()
                        basePlayerDelegate.setProxy(item, uposHost)
                        Toast.makeText(ctx, "已切换代理：${item.name}", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        showEditDialog(index, item)
                    }
                }
                setOnLongClickListener {
                    showEditDialog(index, item)
                    true
                }
            }
            card.addView(TextView(ctx).apply {
                text = item.name
                textSize = 15f
                setTextColor(cfg.foregroundColor)
            })
            card.addView(TextView(ctx).apply {
                text = "Host: ${item.host}  |  ${if (item.isTrust) "信任(携带Token)" else "匿名(不带Token)"}"
                textSize = 12f
                setTextColor(cfg.foregroundAlpha45Color)
            })
            container.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = ctx.dip(8) })
        }
    }

    private fun showEditDialog(index: Int, existing: ProxyServerInfo?) {
        val ctx = requireContext()
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = ctx.dip(16)
            setPadding(p, p, p, 0)
        }
        val nameEt = EditText(ctx).apply {
            hint = "服务器名称（例如：香港节点）"
            setText(existing?.name.orEmpty())
        }
        val hostEt = EditText(ctx).apply {
            hint = "服务器域名（例如：api.example.com）"
            setText(existing?.host.orEmpty())
        }
        val trustCb = CheckBox(ctx).apply {
            text = "信任该服务器（请求时携带登录 Token）"
            isChecked = existing?.isTrust ?: false
        }
        form.addView(nameEt)
        form.addView(hostEt)
        form.addView(trustCb)

        val builder = AlertDialog.Builder(ctx)
            .setTitle(if (index >= 0) "编辑代理服务器" else "添加代理服务器")
            .setView(form)
            .setPositiveButton("保存") { _, _ ->
                val name = nameEt.text?.toString()?.trim().orEmpty()
                val host = hostEt.text?.toString()?.trim()
                    ?.removePrefix("https://")
                    ?.removePrefix("http://")
                    ?.trimEnd('/')
                    .orEmpty()
                if (name.isBlank() || host.isBlank()) {
                    Toast.makeText(ctx, "请填写完整的名称和域名", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                ProxyHelper.saveServer(ctx, ProxyServerInfo(name, host, trustCb.isChecked), index)
                refreshServerList()
            }
            .setNegativeButton("取消", null)

        if (index >= 0) {
            builder.setNeutralButton("删除") { _, _ ->
                ProxyHelper.saveServer(ctx, null, index)
                refreshServerList()
            }
        }
        builder.show()
    }
}
