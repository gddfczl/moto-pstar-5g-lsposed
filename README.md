# Moto Edge S (pstar) 5G DSDS Switcher & Quick Settings Tile
## 摩托罗拉 Edge S (pstar) 双卡 5G/4G 自由切换器与状态栏快捷磁贴

> **Target Device / 目标机型**: Motorola Edge S / Moto G100 (XT2125-4, 内部代号: `pstar`)  
> **Chipset / 芯片平台**: Qualcomm Snapdragon 870 (SM8250-AC) + Snapdragon X55 5G Modem  
> **Supported OS / 支持系统**: LineageOS 21 / 22 / 23.2+ (Android 14 / 15 / 16) with Root (Magisk / KernelSU / APatch)  

---

## 🇨🇳 中文说明

### 1. 项目背景与解决的痛点
Moto Edge S (pstar) 搭载高通骁龙 870 处理器与外挂 X55 基带。受限于高通初代 5G 射频前端架构设计，该硬件仅支持 **DSDS (单通 5G + 4G)**，即**同一时刻仅允许单张 SIM 卡占用 5G 射频通道**。
在升级到 **LineageOS 23.2 (Android 16)** 或 Android 14/15 类原生系统后，系统设置往往对副卡（SIM 2）锁定 5G 选项，或者在切换卡 2 为 5G 时无法自动调度数据通道（DDS），导致副卡无法使用 5G。

本项目通过**直接调用 Android 16 官方 Telephony 服务 AIDL 底层接口**，配合状态栏快捷设置磁贴（QS Tiles），实现了**双卡独立 5G/4G 瞬间秒切与状态实时回显**。

---

### 2. 工作原理剖析

1. **官方二进制网络掩码精准下发**：
   - 在 Android 16 中，系统移除了旧的 `set-allowed-network-types-for-reason`，改为原生标准的：
     ```bash
     cmd phone set-allowed-network-types-for-users -s <SLOT_ID> <NETWORK_TYPES_BITMASK>
     ```
   - **5G 优先 (NR+LTE+CDMA+EVDO+GSM+WCDMA)**: `11001111101111111111`
   - **4G 优先 (LTE+CDMA+EVDO+GSM+WCDMA)**: `01001111101111111111`
   - `-s 0` 对应卡 1 (Phone 0)，`-s 1` 对应卡 2 (Phone 1)。

2. **动态 DDS 智能联动（解决 X55 硬件限制）**：
   - 当用户触发**卡 2 (SIM 2) 开启 5G** 时，代码会自动同步执行 `cmd phone set-default-data-sub-id <SubId>`，将默认数据卡通道迁移至卡 2，确保 X55 的 5G 物理射频通道正确挂载到卡 2；开启卡 1 5G 时自动切回卡 1。

3. **双态防抖与状态锁机制**：
   - 磁贴既是「开关」，也是「状态灯」：
     - 🟢 **磁贴常亮/激活**：代表该卡当前处于 **5G 优先 (NR)** 状态。
     - ⚪ **磁贴熄灭/灰色**：代表该卡当前处于 **4G 优先 (LTE)** 状态。
   - 内置异步防抖锁，防止下拉状态栏的轮询查询与基带切换响应冲突导致的 UI 回弹闪烁。

---

### 3. 安装与使用指引

1. **安装 APK**：
   - 通过本项目 GitHub Actions 自动编译生成的 Release APK，下载并安装到手机。
2. **授予 Root 权限**：
   - 打开应用一次，在弹出的 Magisk / KernelSU / APatch 授权窗口中点击「允许」。
3. **添加下拉栏磁贴**：
   - 手机下拉状态栏两次 $\rightarrow$ 点击 ✏️ 铅笔编辑图标；
   - 在下方未添加区域找到 **「SIM 1 5G/4G」** 与 **「SIM 2 5G/4G」** 磁贴，拖动到顶部常用区域。
4. **日常使用**：
   - 下拉状态栏，点击对应磁贴即可在 5G 与 4G 之间瞬间切换！

---

### 4. Magisk 模块的作用与「不装 Magisk 模块」的注意事项

#### ❓ 不装 Magisk 模块，只安装 APK 是否可以正常使用？
**完全可以正常使用！**  
因为切换核心指令直接由 APK 内部通过 Root 权限向 Android 16 系统下发，不依赖 Magisk 注入。

#### 🧩 Magisk 模块的作用是什么？
Magisk 模块扮演的是**「底层基石与防回弹保险」**：
1. **开机自启初始化**：内置 `service.sh`，在手机冷启动开机的第一时间解除副卡 5G 射频锁定。
2. **CarrierConfig 静态覆盖**：防止运营商配置或系统更新重置双卡 5G 参数。

#### ⚠️ 不装 Magisk 模块时的唯一注意事项：
- **手机重启后**：系统可能默认将网络恢复为系统设置里保存的初始状态。如果重启后发现某张卡没有自动处于 5G，**只需下拉状态栏点击一下对应磁贴，即可立刻切回 5G**。

---
---

## 🇬🇧 English Documentation

### 1. Overview & Problem Solved
The Motorola Edge S (pstar / Moto G100) is powered by the Qualcomm Snapdragon 870 (SM8250-AC) paired with the Snapdragon X55 5G Modem. Due to Qualcomm's first-generation 5G RF front-end architecture, the hardware only supports **DSDS (Dual SIM Dual Standby with Single 5G Active)**, meaning **only one SIM card can access the 5G RF path at any given time**.

On custom ROMs like **LineageOS 23.2 (Android 16)** or Android 14/15, the secondary SIM (SIM 2) is often restricted from enabling 5G in settings, or fails to acquire 5G signal because the default data subscription (DDS) is not routed automatically.

This project utilizes the **native Android 16 Telephony AIDL interface** combined with **Quick Settings (QS) Tiles** to deliver seamless, one-tap 5G/4G switching and real-time state visualization for both SIM slots.

---

### 2. Under the Hood & Architecture

1. **Standardized Android 16 Binary Mask Execution**:
   - Android 16 deprecates the old `set-allowed-network-types-for-reason` command in favor of:
     ```bash
     cmd phone set-allowed-network-types-for-users -s <SLOT_ID> <NETWORK_TYPES_BITMASK>
     ```
   - **5G Preferred (NR|LTE|CDMA|EVDO|GSM|WCDMA)**: `11001111101111111111`
   - **4G Preferred (LTE|CDMA|EVDO|GSM|WCDMA)**: `01001111101111111111`
   - `-s 0` maps to SIM 1 (Phone 0), and `-s 1` maps to SIM 2 (Phone 1).

2. **Dynamic DDS Routing (Overcoming X55 Single-5G Hardware Limit)**:
   - When toggling **SIM 2 to 5G**, the app automatically synchronizes `cmd phone set-default-data-sub-id <SubId>`, routing the cellular data channel to SIM 2 to satisfy Qualcomm's hardware requirement.

3. **QS Tile Dual-Role & Anti-Flicker State Lock**:
   - Tiles act as both a **toggle switch** and a **live status indicator**:
     - 🟢 **Tile Active / Highlighted**: SIM is in **5G Preferred (NR)** mode.
     - ⚪ **Tile Inactive / Dimmed**: SIM is in **4G Preferred (LTE)** mode.
   - Built-in volatile debounce locks prevent race conditions between user clicks and asynchronous system pull-down polling.

---

### 3. Installation & Usage

1. **Install APK**:
   - Download the compiled Release APK from GitHub Actions Artifacts / Releases and install it.
2. **Grant Root Access**:
   - Open the app once and grant Root permission when prompted by Magisk / KernelSU / APatch.
3. **Add QS Tiles**:
   - Swipe down the notification shade twice $\rightarrow$ Tap the ✏️ Edit icon.
   - Drag **"SIM 1 5G/4G"** and **"SIM 2 5G/4G"** into your active Quick Settings panel.
4. **Enjoy Seamless Switching**:
   - Simply tap the tile in your notification bar to switch between 4G and 5G instantly.

---

### 4. Magisk Module Purpose & Standalone App Notes

#### ❓ Can I use ONLY the APK without installing the Magisk module?
**YES, absolutely!**  
The core switching engine operates directly via root-elevated shell commands executed by the APK. It does not strictly require the Magisk module to switch modes.

#### 🧩 What does the Magisk module do?
The Magisk module serves as an **underlying foundation and fallback safeguard**:
1. **Boot Initialization**: Executes `service.sh` during early boot to initialize dual-SIM 5G RF paths.
2. **CarrierConfig Overlay**: Prevents carrier cloud configs from resetting dual-SIM 5G policies.

#### ⚠️ Note when using Standalone APK (without Magisk module):
- **After Device Reboot**: LineageOS might restore the default network preference saved in system settings. If a SIM card does not show 5G after a reboot, **simply tap its QS tile once in the notification shade to restore 5G immediately**.

---

## 📂 Project Structure / 项目工程结构
