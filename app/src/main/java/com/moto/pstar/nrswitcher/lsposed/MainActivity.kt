package com.moto.pstar.nrswitcher.lsposed

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
        }

        val title = TextView(this).apply {
            text = "Moto 5G 双卡切换 (LineageOS 23.2)"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF22C55E.toInt())
        }
        layout.addView(title)

        statusTv = TextView(this).apply {
            text = "\n[正在检测 Root 权限与双卡状态...]"
            textSize = 14f
            setLineSpacing(8f, 1.2f)
        }
        layout.addView(statusTv)

        val btnCheck = Button(this).apply {
            text = "🔄 刷新检测 Root 与双卡 SubId"
            setOnClickListener { refreshStatus() }
        }
        layout.addView(btnCheck)

        val btnSim1 = Button(this).apply {
            text = "⚡ 一键切换 SIM 1 (5G / 4G)"
            setOnClickListener {
                NetworkHelper.toggleSimNetwork(this@MainActivity, 0) { is5G, msg ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        refreshStatus()
                    }
                }
            }
        }
        layout.addView(btnSim1)

        val btnSim2 = Button(this).apply {
            text = "⚡ 一键切换 SIM 2 (5G / 4G 联动数据)"
            setOnClickListener {
                NetworkHelper.toggleSimNetwork(this@MainActivity, 1) { is5G, msg ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        refreshStatus()
                    }
                }
            }
        }
        layout.addView(btnSim2)

        val desc = TextView(this).apply {
            text = "\n【快捷磁贴使用说明】\n1. 下拉状态栏两次 -> 点击 ✏️ 铅笔图标\n2. 在下方找到「SIM 1 5G/4G」与「SIM 2 5G/4G」图标\n3. 拖动至顶部常用区域，即可直接在下拉栏秒切！"
            textSize = 14f
            setLineSpacing(6f, 1.2f)
            setTextColor(0xFF94A3B8.toInt())
        }
        layout.addView(desc)

        scrollView.addView(layout)
        setContentView(scrollView)

        refreshStatus()
    }

    private fun refreshStatus() {
        Thread {
            val rootOk = NetworkHelper.checkRoot()
            val is5GSim1 = NetworkHelper.is5GEnabled(0)
            val is5GSim2 = NetworkHelper.is5GEnabled(1)
            val statusText = buildString {
                append("\n")
                if (rootOk) {
                    append("✓ Root 权限: 已获取 (正常)\n")
                } else {
                    append("✗ Root 权限: 未授权! 请在 Magisk/KernelSU 中授予本应用 Root 权限\n")
                }
                append("• 卡 1 (SIM 1 / Phone 0): 当前 -> ${if (is5GSim1) "5G 优先 (NR)" else "4G 优先 (LTE)"}\n")
                append("• 卡 2 (SIM 2 / Phone 1): 当前 -> ${if (is5GSim2) "5G 优先 (NR)" else "4G 优先 (LTE)"}\n")
            }

            runOnUiThread {
                statusTv.text = statusText
            }
        }.start()
    }
}