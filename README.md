# PreFinal

修复 `GL error 0x0500: Invalid enum at pre-final` —— 让 **Minecraft 1.7.10 + OptiFine** 客户端
不再闪烁、不再刷日志的那个着色器故障。

* 项目 / 模组名：**PreFinal**（mod id `prefinal`）
* 作者：**linakoer** · 版本 **1.0** · 协议 MIT
* 目标：Minecraft 1.7.10 + Forge 10.13.4.1614，仅客户端

详细说明与根因分析：[prefinal/README.md](prefinal/README.md) · [docs/FINDINGS.md](docs/FINDINGS.md)

> English (machine-translated, for reference only): [README.en.md](README.en.md)

## 快速开始

```
cd prefinal
gradlew.bat build              # -> build/libs/prefinal-1.0.jar
gradlew.bat deployToInstance   # 存在 E:/work/mc1710/mods 时拷进去，否则输出到 ./deploy
```

需要 **JDK 8**（1.7.10 线：Gradle 7.4.2 + ForgeGradle 1.2 fork）。

## 开关

所有组件都可以在 `config/prefinal.cfg` 里单独关掉：

| 开关 | 作用 |
|---|---|
| `fix.enableGlStateManagerFix` | 0x0500 修复本体 |
| `errors.enableErrorSuppression` | 拦截 GL 错误（banner / 1283 刷屏） |
| `errors.enableChatFilter` | 过滤聊天栏里的 OpenGL 噪声 |
| `errors.chatFilterPatterns` | 自定义正则列表，留空用内置 |
| `debug.enableGlDebugProbe` | 记录真实 GL 错误调用栈 |
| `debug.selfTest` | 自检：注入一个 GL 错误验证拦截 |

## 发布

| 用途 | 文件 |
|---|---|
| 更新日志 | [CHANGELOG.md](CHANGELOG.md) |
| 商店页面文案（CurseForge / Modrinth，中文权威） | [docs/store-description.md](docs/store-description.md) |
| 同上英文机翻 | [docs/store-description.en.md](docs/store-description.en.md) |
| 上传步骤与字段对照 | [docs/upload-checklist.md](docs/upload-checklist.md) |
| 图标 | `artworks/icon-512.png`、`artworks/icon-1024.png` |
