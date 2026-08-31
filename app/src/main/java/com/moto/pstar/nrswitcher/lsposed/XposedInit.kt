package com.moto.pstar.nrswitcher.lsposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Moto Edge 20 Pro (pstar) LineageOS 23.2 LSPosed 模块入口
 */
class XposedInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "com.android.systemui" -> {
                XposedBridge.log("[Moto5GHook] 命中 SystemUI 进程 (${lpparam.processName})，正在注入原生 QS 磁贴...")
                SystemUiHook.initHook(lpparam.classLoader)
            }
            "com.android.phone" -> {
                XposedBridge.log("[Moto5GHook] 命中 Phone 进程 (${lpparam.processName})，正在激活免 Root 特权射频接口...")
                TelephonyHook.initHook(lpparam.classLoader)
            }
        }
    }
}