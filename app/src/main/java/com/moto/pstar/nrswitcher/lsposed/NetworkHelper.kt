package com.moto.pstar.nrswitcher.lsposed

import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

object NetworkHelper {

    private const val TAG = "Moto5GHelper"
    // Android 16 标准二进制掩码 (bit 19 代表 NR/5G)
    const val BINARY_5G_NR = "11001111101111111111" // 启用 5G NR + 4G + 3G + 2G
    const val BINARY_4G_LTE = "01001111101111111111" // 关闭 5G，仅保留 4G LTE 及以下

    // 防抖状态锁与时间戳，防止系统下拉栏监听与异步切换并发导致的 UI 回弹
    @Volatile var isSwitchingSim1 = false
    @Volatile var isSwitchingSim2 = false
    @Volatile var lastSwitchTimeSim1 = 0L
    @Volatile var lastSwitchTimeSim2 = 0L

    private fun readAllText(stream: InputStream): String {
        return try {
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len = stream.read(buffer)
            while (len != -1) {
                baos.write(buffer, 0, len)
                len = stream.read(buffer)
            }
            baos.toString("UTF-8").trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun checkRoot(): Boolean {
        return try {
            val pair = execSuCommands(arrayOf("id"))
            pair.second.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun execSuCommands(cmds: Array<String>): Pair<Int, String> {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            for (cmd in cmds) {
                os.write(cmd.toByteArray())
                os.write(10) // 写入 '\n'
            }
            val exitCmd = byteArrayOf('e'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 't'.code.toByte())
            os.write(exitCmd)
            os.write(10)
            os.flush()
            os.close()

            val outText = readAllText(process.inputStream)
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
            val res = execSuCommands(arrayOf("cmd phone get-allowed-network-types-for-users -s $slotIndex"))
            // 输出包含 NR 即代表当前槽位 5G 处于开启状态
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

                // 1. 读取当前槽位的真实状态并取反
                val currently5G = is5GEnabled(slotIndex)
                val willEnable5G = !currently5G
                val targetBinaryMask = if (willEnable5G) BINARY_5G_NR else BINARY_4G_LTE

                val cmds = mutableListOf<String>()
                // Moto Edge S (pstar) 单 5G 硬件约束：开启卡 2 5G 时切 DDS 至卡 2；开启卡 1 时切回卡 1
                if (slotIndex == 1 && willEnable5G) {
                    val subId = getSubIdForSlot(context, 1)
                    cmds.add("cmd phone set-default-data-sub-id $subId")
                    cmds.add("settings put global multi_sim_data_call $subId")
                } else if (slotIndex == 0 && willEnable5G) {
                    val subId = getSubIdForSlot(context, 0)
                    cmds.add("cmd phone set-default-data-sub-id $subId")
                    cmds.add("settings put global multi_sim_data_call $subId")
                }

                // 核心：下发 Android 16 官方网络制式掩码
                cmds.add("cmd phone set-allowed-network-types-for-users -s $slotIndex $targetBinaryMask")

                val result = execSuCommands(cmds.toTypedArray())
                Log.d(TAG, "Slot $slotIndex 下发指令完成, exitCode=${result.first}")

                // 延时等待 Telephony 与 RIL 基带完成模式切换
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
