# Modrinth 上架文案（中英双语 · 英文为机翻）

> 用法：下面每一段都标注了粘贴位置。英文部分均为**机器翻译**，以中文为准。

## 1. Summary（粘贴到项目 Summary 输入框，一行）

```
修复 1.7.10 + OptiFine 现代光影（如 BSL）的 GL error 0x0500 每帧刷屏。／Fixes per-frame "GL error 0x0500: Invalid enum at pre-final" spam on 1.7.10 + OptiFine shaders.（英文机翻）
```

## 2. Description（粘贴到项目 Description，整段）

# PreFinal

> **适用声明**
> 本模组**主要修复最新版 BSL 光影在 OptiFine 上使用时**产生的
> `GL error 0x0500: Invalid enum at pre-final` 错误。
> 其他支持 1.7.10 OptiFine 的现代光影包，只要出现**同一个报错**，同样可以用本模组修复。
>
> 已测试组合：**BSL v10 + OptiFine HD U E7 + FalseTweaks 4.4.5** —— 不再报错。

修复 1.7.10 + OptiFine 客户端上 `GL error 0x0500: Invalid enum at pre-final` 造成的日志持续刷屏（实测 40 秒 5216 条）。

## 现象

装了 OptiFine HD U E7 和现代光影包（实测 BSL Classic v10）后进入世界：

- 日志每帧刷 `GL error 0x0500: Invalid enum at pre-final`（实测 40 秒 5216 条）。

## 原因

OptiFine 自带一个 1.8 风格的 `GlStateManager` shim，它在**静态初始化**时从 `OpenGlHelper` 复制四个
framebuffer 枚举。但 OptiFine 的 `Shaders` 类在 GL 上下文创建之前就被加载，于是 `OpenGlHelper`
判定"不支持 framebuffer"，这四个值全部停在 **0**。

之后每帧 `Shaders.renderFinal()` 都会执行：

```java
glFramebufferTexture2D(0 /* 不是 GL_FRAMEBUFFER */, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

产生 `GL_INVALID_ENUM`，final 合成永远盖不到屏幕 framebuffer 上 —— 这就是该错误每帧重复出现的原因。
**与光影包无关，也不是显卡驱动问题。**

## 修复

模组在 GL 上下文就绪后，把 `GlStateManager` 上那四个字段恢复成真实 GL 值：

| 字段 | 正确值 |
|---|---|
| `GL_FRAMEBUFFER` | 36160 |
| `GL_RENDERBUFFER` | 36161 |
| `GL_COLOR_ATTACHMENT0` | 36064 |
| `GL_DEPTH_ATTACHMENT` | 36096 |

并在前若干帧重复施加，以应对 shim 的延迟初始化。

## 顺带处理（都能单独关）

- **GL 错误拦截**：每帧结束清空挂起的 GL 错误，这样 Minecraft 自己的轮询就不会再打印
  `########## GL ERROR ########## @ Pre render` 和 1283 刷屏。被吃掉的错误码会统计并记录到
  `gldebug-probe.log`，非白名单错误会被明确标出，不会掩盖新问题。
- **聊天栏过滤**：取消进入聊天栏的 OpenGL 报错文本（内置 9 条正则，可自定义）。
- **诊断探针**（默认关）：同步 `KHR_debug` 回调，记录任何真实 GL 错误的调用栈。

## 安装

1. 需要 **Forge 1.7.10（10.13.4.1614）** 客户端；
2. 把 `prefinal-1.0.jar` 放进 `mods/`；
3. 仅客户端需要，服务器不用装。

## 配置

`config/prefinal.cfg`（首次启动自动生成）：

```
fix    { B:enableGlStateManagerFix=true }        # 修复本体
errors { B:enableErrorSuppression=true           # 拦截 GL 错误
         B:enableChatFilter=true                 # 聊天栏过滤
         S:chatFilterPatterns < > }              # 自定义正则，留空用内置
debug  { B:enableGlDebugProbe=false              # 调用栈诊断
         B:enableSelfTest=false }                # 自检
```

## 实测数据

环境：MC 1.7.10 + Forge 10.13.4.1614 + OptiFine HD U E7 + BSL Classic v10，73 个模组的整合包。

| 指标 | 修复前 | 修复后 |
|---|---|---|
| `GL error 0x0500` | 40 秒 5216 条 | **0** |
| `########## GL ERROR` banner | 持续刷 | **0** |
| 光影 program | 29 个照常编译 | 29 个照常编译，画面稳定 |

## 兼容性

- 纯客户端 mod，使用 Forge 事件总线与反射，不使用 Mixin，可与 mixin 类模组共存（实测与 UniMixins、
  Shader Fixer、FalseTweaks 同装无冲突）。
- 依赖 OptiFine 的类结构；其它 1.7.10 光影实现未经测试。
- 只修 GL 状态初始化顺序这一处，光影包自身的其它告警（如 `DRAWBUFFERS` 越界）不在范围内。

## 不修复的范围（明确声明）

本模组**只**处理 OptiFine `GlStateManager` 初始化顺序导致的 `GL error 0x0500: Invalid enum at
pre-final`，以及它带来的日志/聊天刷屏。下列问题**不在修复范围，也不会被修复**：

* **BSL Classic 版**在 OptiFine 上出现的**阴影拉长/错位、画面抖动**（Classic 是按旧版 shadersmod
  调参的，请改用**非 Classic 版** BSL v10）。
* **任何由光影包自身**（版本、配置、内置选项）与 OptiFine 不匹配引起的异常：画面错位、闪烁、
  阴影错误、着色器编译失败、选项解析告警等。
* **第三方模组之间**的冲突，例如 Shader Fixer 与 JourneyMap 版本不匹配导致启动崩溃。

**判断标准：凡是"换一个光影包 / 换一个模组版本就好"的问题，都不属于本模组职责。**

## 许可与致谢

MIT 协议。不使用、不打包任何 OptiFine / 游戏 / 其它模组的代码或资源；只在你本地运行时修正
OptiFine 的四个静态字段。感谢 OptiFine 作者与光影作者们的长期维护。

> 英文版为机器翻译，仅供参考；以本中文说明为准。

---

## English（以下为机器翻译 / machine translation，仅供参考，以中文为准）

## PreFinal (English · machine translation)

# PreFinal

> **Scope**
> This mod **primarily fixes `GL error 0x0500: Invalid enum at pre-final`** produced by
> **current BSL shaders used with OptiFine**. Any other modern shaderpack for 1.7.10 OptiFine that
> produces **the same error** can be repaired by it as well.
>
> Tested with: **BSL v10 + OptiFine HD U E7 + FalseTweaks 4.4.5** — no more errors.

Fixes the log spam caused by `GL error 0x0500: Invalid enum at pre-final` on
Minecraft 1.7.10 + OptiFine clients.

## Symptom

With OptiFine HD U E7 and a modern shaderpack (tested with BSL Classic v10), entering a world:

- floods the log with `GL error 0x0500: Invalid enum at pre-final` (5216 lines in 40 seconds).

## Cause

OptiFine ships a 1.8-style `GlStateManager` shim that copies four framebuffer enums out of
`OpenGlHelper` in its **static initialiser**. OptiFine's `Shaders` class is loaded before a GL
context exists, so `OpenGlHelper` reports "no framebuffer support" and all four values stay **0**.

Every frame afterwards, `Shaders.renderFinal()` runs:

```java
glFramebufferTexture2D(0 /* not GL_FRAMEBUFFER */, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

which raises `GL_INVALID_ENUM`, so the final composite never reaches the screen framebuffer — that
is why the error repeats every frame. **Not shaderpack specific, not a driver problem.**

## Fix

Once a GL context is current, the mod restores the four fields on `GlStateManager` to their real
GL values (36160 / 36161 / 36064 / 36096), re-applying for the first frames to cover lazy
initialisation.

## Extras (each can be switched off)

- **GL error suppression**: pending GL errors are drained at frame end so Minecraft's own poll no
  longer prints `########## GL ERROR ########## @ Pre render` and the 1283 spam. Drained codes are
  counted in `gldebug-probe.log`; non-whitelisted codes are flagged so nothing is hidden.
- **Chat filter**: cancels OpenGL error text that would reach the chat bar (9 built-in patterns,
  configurable).
- **Debug probe** (off by default): a synchronous `KHR_debug` callback that records real GL error
  call stacks.

## Install

1. Forge **1.7.10 (10.13.4.1614)** client required;
2. drop `prefinal-1.0.jar` into `mods/`;
3. client only, nothing to install on a server.

## Config

`config/prefinal.cfg` (generated on first start) exposes `enableGlStateManagerFix`,
`enableErrorSuppression`, `enableChatFilter`, `chatFilterPatterns`, `enableGlDebugProbe` and
`selfTest`.

## Measured

MC 1.7.10 + Forge 10.13.4.1614 + OptiFine HD U E7 + BSL Classic v10 in a 73-mod pack:
`GL error 0x0500` 5216 in 40 s → **0**; banner **0**; all 29 shader programs still compile.

## Compatibility

Client-side mod using the Forge event bus and reflection, no Mixin; verified to coexist with
UniMixins, Shader Fixer and FalseTweaks. Relies on OptiFine's class layout; other 1.7.10 shader
implementations are untested. Only the GL state initialisation order is repaired.

## Out of scope (explicit)

This mod **only** repairs `GL error 0x0500: Invalid enum at pre-final` caused by OptiFine's
`GlStateManager` initialisation order, plus the log/chat spam it produces. The following are **not
fixed and will not be fixed**:

* **BSL Classic** on OptiFine: **stretched/misplaced shadows and jitter** (Classic is tuned for old
  shadersmod conventions; use the **non-Classic** BSL v10).
* **Any artifact coming from the shaderpack itself** (version, settings, built-in options) not
  matching OptiFine: misplaced geometry, flicker, wrong shadows, shader compile failures, option
  parsing warnings.
* **Third-party mod conflicts**, e.g. Shader Fixer vs JourneyMap version mismatch crashing startup.

**Rule of thumb: anything that goes away by switching the shaderpack or a mod version is out of
scope.**

## Licence

MIT. No OptiFine / game / mod code or assets are bundled; the mod only corrects four static fields
of OptiFine at runtime on your own machine.

---

## 3. Changelog（粘贴到版本上传的 Changelog）

首个发布版本。

**修复**

* 修复 `GL error 0x0500: Invalid enum at pre-final`：OptiFine 的 `GlStateManager` shim 在 GL
  上下文存在之前完成静态初始化，四个 framebuffer 枚举（`GL_FRAMEBUFFER` / `GL_RENDERBUFFER` /
  `GL_COLOR_ATTACHMENT0` / `GL_DEPTH_ATTACHMENT`）全部停在 0，导致 `Shaders.renderFinal()` 每帧
  调用 `glFramebufferTexture2D(0, ...)` 触发 `GL_INVALID_ENUM`，final 合成无法贴到屏幕 framebuffer。
* 该错误不再每帧刷新（实测 40 秒 5216 条 → 0）。

**顺带处理（可单独关闭）**

* 在每帧结束时清空挂起的 GL 错误，使 Minecraft 自己的轮询不再打印
  `########## GL ERROR ########## @ Pre render` 与 1283 刷屏；被消耗的错误码会统计并记录。
* 过滤聊天栏中的 OpenGL 报错文本（内置 9 条正则，可自定义）。
* 可选的同步 `KHR_debug` 探针，把真实 GL 错误的调用栈写入 `gldebug-probe.log`。

**配置**

* 新增 `config/prefinal.cfg`，修复本体、错误拦截、聊天过滤、诊断探针、自检均可独立开关。

**已知限制**

* 仅支持 Minecraft 1.7.10 + Forge（客户端）；针对 OptiFine 的类结构，未测试其它 1.7.10 光影实现。
* 只修 GL 状态初始化顺序这一处；其它 1.7.10 光影问题（例如光影包自身的 `DRAWBUFFERS` 越界告警）不在范围内。

**English (machine translation) / 英文机翻**

First release.

**Fixed**

* `GL error 0x0500: Invalid enum at pre-final`: OptiFine's `GlStateManager` shim initialises four
  framebuffer enums before a GL context exists, so they stay 0 and `Shaders.renderFinal()` calls
  `glFramebufferTexture2D(0, ...)` every frame, raising `GL_INVALID_ENUM`; the final composite never
  reaches the screen framebuffer.
* The error no longer refreshes every frame (5216 lines in 40 s -> 0).

**Incidental, individually switchable**

* Drains pending GL errors at frame end so Minecraft's own poll no longer prints
  `########## GL ERROR ########## @ Pre render` and the 1283 spam; drained codes are counted and logged.
* Filters OpenGL error text out of the chat bar (9 built-in patterns, configurable).
* Optional synchronous `KHR_debug` probe writing real GL call stacks to `gldebug-probe.log`.

**Config**

* New `config/prefinal.cfg`; the repair, error suppression, chat filter, debug probe and self-test
  can each be toggled independently.

**Known limits**

* Minecraft 1.7.10 + Forge client only; relies on OptiFine's class layout, other 1.7.10 shader
  implementations are untested.
* Fixes only the GL state initialisation order; unrelated 1.7.10 shader issues (e.g. a pack's own
  out-of-range `DRAWBUFFERS` warning) are out of scope.
