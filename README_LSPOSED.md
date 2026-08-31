# Moto Edge 20 Pro 原生 5G/4G QS 磁贴 LSPosed 模块

本模块专为 **Moto Edge 20 Pro (pstar) / LineageOS 23.2 (Android 16)** 深度定制。

## 为什么选择 LSPosed 模块？
1. **纯原生 SystemUI 磁贴注入**：直接将 SIM 1 5G 与 SIM 2 5G 开关植入系统原生下拉通知栏磁贴池，与系统自带的 Wifi、蓝牙磁贴无缝集成。
2. **0 延迟特权执行**：直接运行在 `com.android.phone` 进程内部，免去每次弹窗或调用外部 Root Shell 的耗时。
3. **无缝兼顾 DSDS 5G 硬件限制**：点击副卡 5G 磁贴时，在系统底层同步将数据通道 (DDS) 瞬移至副卡，彻底解决副卡开 5G 无法握手基带的痛点。

## 安装与激活流程
1. 在手机上安装本模块 APK (或通过 GitHub Actions 1分钟在线自动编译出 APK)。
2. 打开 **LSPosed Manager**。
3. 在模块列表中找到 **「Moto 5G DSDS QS Hook」**。
4. 勾选启用模块，并确保勾选以下作用域 (Scope)：
   - `系统界面 (com.android.systemui)`
   - `电话服务 (com.android.phone)`
5. 点击 LSPosed 右上角菜单 -> **重启 SystemUI** (或软重启手机)。
6. 下拉通知栏 -> 点击编辑铅笔图标 -> 将 **「SIM 1 网络制式」** 与 **「SIM 2 网络制式」** 磁贴拖至常用区域！
