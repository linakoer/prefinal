# 更新日志

## 1.0 — 2026-11-08

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

---

## 1.0 — 2026-11-08 (English summary, machine-translated)

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
