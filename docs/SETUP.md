# Validator 与 Android Probe 环境配置

## validator（HTTP / Browser / Auto 验证）

必需：

- Java 17+
- 可访问目标网站的网络环境

启动：

```powershell
cd .\legado-book-source-generator\validator
.\run.bat
```

浏览器打开 `http://localhost:1111`。

停止：

```powershell
# run.bat 窗口里按 Ctrl+C
# 或者
.\stop.bat
```

---

## Android WebView Probe（可选）

用于复核带 `webView:true` / `webJs` 的书源链路。运行在真实 Android WebView 上，比桌面 Browser 模式更接近阅读 App，但仍不等于阅读 App 100% 通过。

需要：

- 一台打开 USB 调试的 Android 真机，或一个已启动的 Android 模拟器
- `adb`（可用 Release 包内的 `setup-adb.bat` 自动下载）
- Release 包内置的 `validator\android-probe.apk`

### adb 自动查找顺序

validator 会按顺序自动查找：

1. `validator\tools\platform-tools\adb.exe`（`setup-adb.bat` 安装位置）
2. `ANDROID_HOME\platform-tools\adb.exe`
3. `ANDROID_SDK_ROOT\platform-tools\adb.exe`
4. `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
5. `PATH` 里的 `adb`

Windows 上常见 ADB 路径：

```text
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
```

### 自动安装 adb

```powershell
cd .\legado-book-source-generator\validator
.\setup-adb.bat
```

`setup-adb.bat` 会从 Google 官方地址下载 Windows Platform-Tools，并解压到当前 Release 包的 `validator\tools\platform-tools\`。它不会把 `adb.exe` 提交进仓库，也不会写入系统目录。

### 手动配置

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
setx PATH "$env:PATH;$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

设置用户环境变量后，新开的终端/程序才会继承。

### 安装并启动 Probe

```powershell
cd .\legado-book-source-generator\validator
.\setup-android-probe.bat
```

脚本会执行：

- 检查 `adb`，缺失时自动调用 `setup-adb.bat`
- 查找连接设备
- 安装 `android-probe.apk`
- 启动 `io.legado.probe/.WebViewProbeActivity`
- 建立 `localhost:18888 -> device:18888` 端口转发

### 没有设备时

validator 会返回：

```text
validator_limitation
Android Probe 不可用: No Android devices connected
```

这不是书源失败，而是当前电脑没有可用 Android WebView 复核环境。
