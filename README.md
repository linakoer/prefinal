# PreFinal

> **适用声明**
> 本模组**主要修复最新版 BSL 光影在 OptiFine 上使用时**产生的
> `GL error 0x0500: Invalid enum at pre-final` 错误。
> 其他支持 1.7.10 OptiFine 的现代光影包，只要出现**同一个报错**，同样可以用本模组修复。
>
> 已测试组合：**BSL v10 + OptiFine HD U E7 + FalseTweaks 4.4.5** —— 不再报错。

修复 `GL error 0x0500: Invalid enum at pre-final` —— 针对 **Minecraft 1.7.10 + OptiFine** 客户端
那个**每帧持续刷新**、把日志刷爆的着色器错误。

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

## 不修复的范围（明确声明）

本模组**只**处理 OptiFine `GlStateManager` 初始化顺序导致的 `GL error 0x0500: Invalid enum at pre-final`
以及它带来的日志/聊天刷屏。以下问题**不修**，请向对应光影包/模组作者反馈：

* **BSL Classic 版**在 OptiFine 上的**阴影拉长/错位、画面抖动** —— 请改用**非 Classic 版** BSL v10。
* **光影包自身**（版本/配置/内置选项）与 OptiFine 不匹配引发的异常：画面错位、闪烁、阴影错误、着色器编译失败等。
* **第三方模组之间**的冲突（例如 Shader Fixer 与 JourneyMap 版本不匹配导致启动崩溃）。

**判断标准：凡是"换一个光影包/换一个模组版本就好"的问题，都不属于本模组职责。**