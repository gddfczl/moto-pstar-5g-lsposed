package com.moto.pstar.nrswitcher.lsposed

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 运行在 com.android.phone 进程中的特权操作器
 * 进程持有系统 android.uid.phone (UID 1001)，具备直接操控 RIL 射频的能力
 */
object TelephonyHook {

    private const val BITMASK_5G_NR = 992850L // NR | LTE | WCDMA | GSM
    private const val BITMASK_4G_LTE = 468562L // LTE | WCDMA | GSM

    fun initHook(classLoader: ClassLoader) {
        try {
            val phoneFactoryClass = XposedHelpers.findClassIfExists(
                "com.android.internal.telephony.PhoneFactory",
                classLoader
            )
            
            if (phoneFactoryClass != null) {
                XposedBridge.log("[Moto5GHook] 电话服务 PhoneFactory 就绪，特权射频执行通道已打通。")
            }

        } catch (t: Throwable) {
            XposedBridge.log("[Moto5GHook] TelephonyHook 异常: " + t.message)
        }
    }

    /**
     * 进程内特权切换：免 Root 命令行，无 IPC 耗时
     */
    fun switchSubNetworkMode(telephonyManager: TelephonyManager, subId: Int, enable5G: Boolean) {
        try {
            val bitmask = if (enable5G) BITMASK_5G_NR else BITMASK_4G_LTE
            
            // 直接调用 TelephonyManager.setAllowedNetworkTypesForReason (Reason 0 = USER)
            telephonyManager.createForSubscriptionId(subId).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                bitmask
            )
            XposedBridge.log("[Moto5GHook] 已对 subId=$subId 下发网络类型 bitmask=$bitmask")
        } catch (e: Exception) {
            XposedBridge.log("[Moto5GHook] switchSubNetworkMode 失败: " + e.message)
        }
    }
}