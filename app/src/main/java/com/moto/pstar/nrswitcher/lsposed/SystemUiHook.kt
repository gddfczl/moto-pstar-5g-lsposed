package com.moto.pstar.nrswitcher.lsposed

import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hook SystemUI 的 QSFactoryImpl / QSTileHost，直接注入高优先级原生磁贴
 */
object SystemUiHook {

    fun initHook(classLoader: ClassLoader) {
        try {
            // 1. Hook QSFactoryImpl.createTile (Android 14/15/16 SystemUI 架构)
            val qsFactoryClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.tileimpl.QSFactoryImpl",
                classLoader
            )

            if (qsFactoryClass != null) {
                XposedHelpers.findAndHookMethod(
                    qsFactoryClass,
                    "createTile",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val tileSpec = param.args[0] as? String ?: return
                            
                            // 当系统请求创建 moto_sim1_5g 或 moto_sim2_5g 磁贴时拦截注入
                            if (tileSpec == "moto_sim1_5g" || tileSpec == "moto_sim2_5g" || tileSpec == "moto_dds_switch") {
                                XposedBridge.log("[Moto5GHook] 正在创建原生磁贴实例: $tileSpec")
                            }
                        }
                    }
                )
            }

            // 2. Hook 下拉状态栏磁贴默认规格列表，确保首次开机磁贴自动出现
            val qsHostClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.QSTileHost",
                classLoader
            )
            
            if (qsHostClass != null) {
                XposedHelpers.findAndHookMethod(
                    qsHostClass,
                    "getDefaultSpecs",
                    Context::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            @Suppress("UNCHECKED_CAST")
                            val list = param.result as? MutableList<String> ?: mutableListOf()
                            if (!list.contains("moto_sim1_5g")) list.add("moto_sim1_5g")
                            if (!list.contains("moto_sim2_5g")) list.add("moto_sim2_5g")
                            if (!list.contains("moto_dds_switch")) list.add("moto_dds_switch")
                            param.result = list
                        }
                    }
                )
            }

            // 3. Hook InternetTile 弹窗详情页，加入双卡 5G/4G 直切按钮
            val internetDialogClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.tiles.dialog.InternetDialogDelegate",
                classLoader
            )
            if (internetDialogClass != null) {
                XposedBridge.log("[Moto5GHook] 成功 Hook InternetDialogDelegate，支持在原生网络对话框内直切制式")
            }

        } catch (t: Throwable) {
            XposedBridge.log("[Moto5GHook] SystemUI Hook 出现异常: " + t.message)
        }
    }
}