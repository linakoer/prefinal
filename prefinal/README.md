# PreFinal

修复 Minecraft **1.7.10 + OptiFine** 下的着色器错误

```
GL error 0x0500: Invalid enum at pre-final
```

作者 **linakoer** · 版本 **1.0** · mod id `prefinal` · MIT 协议

> English (machine-translated): [README.en.md](README.en.md) — 英文版为机器翻译，以本文为准。

## 问题现象

在 OptiFine HD U E7 + 现代光影包（实测 BSL Classic v10）的 1.7.10 客户端里进入世界后：

* 日志疯狂刷 `GL error 0x0500: Invalid enum at pre-final`（40 秒 5216 条）。

**根因**（用同步 GL 调试回调定位，详见 [docs/FINDINGS.md](docs/FINDINGS.md)）：
OptiFine 自带一个 1.8 风格的 `GlStateManager` shim，它在静态初始化时从 `OpenGlHelper` 复制四个
framebuffer 枚举。而 OptiFine 的 `Shaders` 类在 GL 上下文创建之前就被加载，于是 `OpenGlHelper`
判定"不支持 framebuffer"，这四个值全部停在 **0**。此后每帧 `Shaders.renderFinal()` 都会调用

```java
glFramebufferTexture2D(0 /* 不是 GL_FRAMEBUFFER */, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

产生 `GL_INVALID_ENUM`，final 合成永远盖不到屏幕 framebuffer 上。

这与光影包无关，也不是驱动问题。

## 模组做了什么

| 组件 | 作用 |
|---|---|
| `GlStateManagerFix` | **真正的修复**——GL 上下文就绪后，把 shim 上的 `GL_FRAMEBUFFER` / `GL_RENDERBUFFER` / `GL_COLOR_ATTACHMENT0` / `GL_DEPTH_ATTACHMENT` 恢复成真实 GL 值（36160 / 36161 / 36064 / 36096），并在前若干帧重复施加以防延迟初始化 |
| `GLErrorSuppressor` | 阻止 Minecraft 自己的 `runTick` 轮询打印 `########## GL ERROR ########## @ Pre render` 与 1283 刷屏；被吃掉的错误码会统计并记录，不会静默丢失 |
| `ChatSpamFilter` | 取消会进入聊天栏的 OpenGL 噪声文本（正则可配） |
| `GLDebugProbe` | 可选诊断：同步 `KHR_debug` 回调，把真实错误的调用栈写进 `gldebug-probe.log` |

第一行是这个模组存在的理由；其余是顺带的，避免同一次故障淹没日志和聊天栏。

## 安装

把 `prefinal-1.0.jar` 丢进 Forge 1.7.10 客户端的 `mods/`。仅客户端需要
（`acceptableRemoteVersions = "*"`，放服务端也无害）。

## 配置

只有一个生成文件：**`config/prefinal.cfg`**（首次启动自动生成、带注释，游戏内可在模组配置里编辑）。

```
fix {
    B:enableGlStateManagerFix=true       # 0x0500 修复本体；关掉可以直接看到错误回来
}

errors {
    B:enableErrorSuppression=true        # 拦截 GL 错误，banner / 1283 刷屏不再出现
    B:enableChatFilter=true              # 过滤聊天栏里的 OpenGL 噪声
    S:chatFilterPatterns <               # 可选正则列表，留空则使用内置 9 条
     >
}

debug {
    B:enableGlDebugProbe=false           # KHR_debug 调用栈 -> gldebug-probe.log
    B:selfTest=false                     # 注入一个 GL 错误，验证拦截是否生效
}
```

各组件相互独立，可单独关闭；启动时会打印实际启用的组合：

```
[PreFinal] config: fix=true errorSuppression=true chatFilter=true (built-in patterns) debugProbe=false selfTest=false
```

改动后需**重启游戏**生效。

## 构建

需要 **JDK 8**（1.7.10 线：Gradle 7.4.2 + ForgeGradle 1.2 fork）。

```
gradlew.bat build
gradlew.bat deployToInstance    # 若存在 E:/work/mc1710/mods 则拷进去，否则输出到 ./deploy
```

`gradle.properties` 里钉了 `org.gradle.java.home`，改成你自己的 JDK 8 路径。

## 验证记录

环境：MC 1.7.10 + Forge 10.13.4.1614 + OptiFine HD U E7 + BSL Classic v10，73 个模组的整合包。

| 指标 | 结果 |
|---|---|
| `GL error 0x0500` 条数 | 40 秒 5216 条 → **0** |
| `########## GL ERROR` banner | **0** |
| 光影 program 编译 | 29 / 29 |

## 不修复的范围（明确声明）

本模组**只**处理 OptiFine `GlStateManager` 初始化顺序导致的 `GL error 0x0500: Invalid enum at
pre-final`，以及它带来的日志/聊天刷屏。下列问题**不在修复范围，也不会被修复**，请向对应的
光影包或模组作者反馈：

* **BSL Classic 版**在 OptiFine 上出现的**阴影拉长/错位、画面抖动**。Classic 是按旧版
  shadersmod 的约定调参的，与 OptiFine 的管线假设不一致；请改用**非 Classic 版** BSL v10。
* **任何由光影包自身**（版本、配置、内置选项）与 OptiFine 不匹配引起的异常，例如画面错位、
  闪烁、阴影错误、着色器编译失败、选项解析告警等。
* 其它**第三方模组之间**的冲突，例如 Shader Fixer 与 JourneyMap 版本不匹配导致启动崩溃。

换句话说：**凡是"换一个光影包/换一个模组版本就好"的问题，都不属于本模组职责。**

## 协议

MIT，见 [LICENSE](../LICENSE)。与第三方的交互说明见 [NOTICE.md](../NOTICE.md)；本仓库不重新分发任何游戏或模组资源。
