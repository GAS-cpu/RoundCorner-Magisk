# 屏幕圆角（Round Corner）Magisk 模块

让直角屏幕的手机，屏幕四角显示圆角，视觉上接近圆角屏。

## 适配环境

- Android 5.0+（实测于 **Android 6.0 MIUI**）
- Magisk 20.4+
- 无需任何特殊权限

## 功能

- 屏幕四角绘制黑色圆角遮罩，形成视觉圆角屏
- 开机自启（声明了 `RECEIVE_BOOT_COMPLETED` 权限）
- 手动启动兜底（桌面「圆角屏幕」图标，点一下立即生效）

## 使用方法

1. 用 Magisk「从本地安装」刷入 `RoundCorner-Magisk25.zip`
2. 重启手机
3. 桌面出现「**圆角屏幕**」图标，**点一次**（提示「屏幕圆角已启用」即生效，同时激活开机自启）

> 只重启不点图标也能自动生效；点图标是为了「激活应用」，确保后续开机自启稳定。

## 自定义圆角大小

修改 `src/com/shizuku/roundcorner/CornerService.java` 里的常量：

```java
private static final int CORNER_RADIUS_DP = 16;  // 圆角半径，单位 dp，越大圆角越明显
```

改完后重新编译打包即可（见下方「编译」）。

## 技术实现

- **原理**：用 `WindowManager` 添加一个全屏悬浮窗，在四个角绘制黑色圆角遮罩（`Path` 画「直角区域减去四分之一圆」）
- **悬浮窗类型**：优先 `TYPE_SYSTEM_ERROR`（常驻、不闪烁）；权限不足时回退 `TYPE_TOAST`（双缓冲交替刷新，避免直角闪烁）
- **结构**：

```
├── module.prop                     # 模块信息
├── META-INF/com/google/android/
│   ├── update-binary               # Magisk 安装脚本
│   └── updater-script              # 占位
└── system/app/RoundCorner/
    └── RoundCorner.apk             # 圆角遮罩 App（系统应用，非 priv-app，避免 platform 签名要求）
```

- **App 源码**（`src/com/shizuku/roundcorner/`）：
  - `CornerOverlayView.java` — 圆角遮罩绘制（四角，用 scale 镜像复用左上角逻辑）
  - `CornerService.java` — 前台服务，管理悬浮窗生命周期
  - `MainActivity.java` — 手动启动入口
  - `BootReceiver.java` — 开机自启

## 编译

工具链要求：Android SDK（aapt2 / d8 / zipalign / apksigner）+ JDK。

```bash
BT="<sdk>/build-tools/37.0.0"
AJ="<sdk>/platforms/android-34/android.jar"

# 1. 链接资源
"$BT/aapt2.exe" link -o build/app_base.apk -I "$AJ" \
    --manifest AndroidManifest.xml --min-sdk-version 21 --target-sdk-version 23

# 2. 编译 Java
javac -source 8 -target 8 -classpath "$AJ" -d build/classes \
    src/com/shizuku/roundcorner/*.java

# 3. 转 dex
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --lib "$AJ" \
    --min-api 21 --output build/dex build/classes/com/shizuku/roundcorner/*.class

# 4. 把 classes.dex 塞进 APK（用任意 zip 工具或 python zipfile）
# 5. zipalign
"$BT/zipalign.exe" -f 4 build/app_base.apk build/app_aligned.apk

# 6. 签名（自生成 keystore 即可，非 priv-app 不需要 platform 签名）
keytool -genkeypair -keystore roundcorner.jks -alias roundcorner \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass roundcorner123 -keypass roundcorner123 \
    -dname "CN=RoundCorner, OU=Dev, O=Shizuku, L=Beijing, ST=Beijing, C=CN"
"$BT/apksigner.bat" sign --ks roundcorner.jks --ks-pass pass:roundcorner123 \
    --key-pass pass:roundcorner123 --out build/app.apk build/app_aligned.apk
```

然后把 `build/app.apk` 复制为 `module/system/app/RoundCorner/RoundCorner.apk`，重新打包 `module/` 目录为 zip。

## 卸载

Magisk → 模块 → 移除「屏幕圆角」模块 → 重启。数据无残留。

## 常见问题

| 现象 | 处理 |
|---|---|
| 重启后没圆角 | 点一次桌面「圆角屏幕」图标 |
| 圆角一闪一闪 | 极少数机型 `TYPE_SYSTEM_ERROR` 拿不到权限，回退到 `TYPE_TOAST` 双缓冲；若仍可见闪烁，把刷新间隔调小 |
| 圆角太大/太小 | 改 `CORNER_RADIUS_DP` 后重新编译 |

## 说明

这是「视觉圆角」——把屏幕四角涂成黑色（模拟屏幕黑边），不是真正裁剪显示区域。对屏幕四周有黑边的机型（如 OPPO R9m）效果自然；若你的机型边框是白色，可把遮罩颜色 `Color.BLACK` 改成对应的边框色。
