package com.moto.pstar.nrswitcher.lsposed

import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object NetworkHelper {

    private const val TAG = "Moto5GHelper"
    // Android 16 标准二进制掩码
    const val BINARY_5G_NR = "11001111101111111111" // NR|LTE|CDMA|EVDO|GSM|WCDMA
    const val BINARY_4G_LTE = "01001111101111111111" // LTE|CDMA|EVDO|GSM|WCDMA

    // 防抖与操作时间戳，防止系统下拉栏监听与异步切换并发导致的 UI 回弹
    @Volatile var isSwitchingSim1 = false
    @Volatile var isSwitchingSim2 = false
    @Volatile var lastSwitchTimeSim1 = 0L
    @Volatile var lastSwitchTimeSim2 = 0L

    fun checkRoot(): Boolean {
        return try {
            val pair = execSuCommands(listOf("id"))
            pair.second.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun execSuCommands(cmds: List<String>): Pair<Int, String> {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in cmds) {
                os.write(cmd.toByteArray())
                os.write(10)
            }
            os.write("exit".toByteArray())
            os.write(10)
            os.flush()

            val sb = java.lang.StringBuilder()
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = br.readLine()
            while (line != null) {
                sb.append(line).append("
")
                line = br.readLine()
            }
            br.close()
            val outText = sb.toString().trim()
            val exitCode = process.waitFor()
            Pair(exitCode, outText)
        } catch (e: Exception) {
            Log.e(TAG, "execSuCommands 异常: ${e.message}")
            Pair(-1, e.message ?: "")
        }
    }

    fun getSubIdForSlot(context: Context, slotIndex: Int): Int {
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val info = sm?.getActiveSubscriptionInfoForSimSlotIndex(slotIndex)
            info?.subscriptionId ?: (slotIndex + 1)
        } catch (e: Exception) {
            slotIndex + 1
        }
    }

    fun is5GEnabled(slotIndex: Int): Boolean {
        return try {
            val res = execSuCommands(listOf("cmd phone get-allowed-network-types-for-users -s $slotIndex"))
            // LineageOS 23.2 输出中包含 NR 则代表 5G 处于允许启用状态
            res.second.contains("NR")
        } catch (e: Exception) {
            false
        }
    }

    fun toggleSimNetwork(context: Context, slotIndex: Int, onComplete: ((Boolean, String) -> Unit)? = null) {
        Thread {
            try {
                if (slotIndex == 0) {
                    isSwitchingSim1 = true
                    lastSwitchTimeSim1 = System.currentTimeMillis()
                } else {
                    isSwitchingSim2 = true
                    lastSwitchTimeSim2 = System.currentTimeMillis()
                }

                // 1. 读取当前真实状态
                val currently5G = is5GEnabled(slotIndex)
                val willEnable5G = !currently5G
                val targetBinaryMask = if (willEnable5G) BINARY_5G_NR else BINARY_4G_LTE

                val cmds = mutableListOf<String>()
                // 针对 Moto Edge S (pstar) 单 5G 硬件限制：若卡 2 (Slot 1) 开启 5G，将默认数据卡移至卡 2
                if (slotIndex == 1 && willEnable5G) {
                    val subId = getSubIdForSlot(context, 1)
                    cmds.add("cmd phone set-default-data-sub-id $subId")
                    cmds.add("settings put global multi_sim_data_call $subId")
                } else if (slotIndex == 0 && willEnable5G) {
                    // 若卡 1 (Slot 0) 开启 5G，将默认数据卡移回卡 1
                    val subId = getSubIdForSlot(context, 0)
                    cmds.add("cmd phone set-default-data-sub-id $subId")
                    cmds.add("settings put global multi_sim_data_call $subId")
                }

                // 核心：下发 Android 16 官方网络制式掩码
                cmds.add("cmd phone set-allowed-network-types-for-users -s $slotIndex $targetBinaryMask")

                val result = execSuCommands(cmds)
                Log.d(TAG, "Slot $slotIndex 下发指令完成, exitCode=${result.first}")

                // 适当延时等待 Telephony 与 RIL 基带完成模式切换
                Thread.sleep(400)

                // 2. 再次校验最新真实状态
                val confirmed5G = is5GEnabled(slotIndex)
                val modeStr = if (confirmed5G) "5G 优先 (NR)" else "4G 优先 (LTE)"
                val msg = "SIM ${slotIndex + 1} 已切换至: $modeStr"

                onComplete?.invoke(confirmed5G, msg)
            } catch (e: Exception) {
                Log.e(TAG, "toggleSimNetwork 失败: ${e.message}")
                onComplete?.invoke(false, "切换异常: ${e.message}")
            } finally {
                Thread.sleep(500)
                if (slotIndex == 0) isSwitchingSim1 = false else isSwitchingSim2 = false
            }
        }.start()
    }
}