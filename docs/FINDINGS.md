# 结论：`GL error 0x0500: Invalid enum at pre-final` 的根因与修复

环境：MC 1.7.10 + Forge 10.13.4.1614 + OptiFine HD U E7 + BSL Classic v10.0
GPU / 驱动：NVIDIA GeForce GTX 1070 / 582.78，OpenGL 4.6.0

## 现象

进入世界加载 BSL 后，`logs` 与 stderr 每帧刷数百条：

```
GL error 0x0500: Invalid enum at pre-final
```

40 秒内 5216 条；画面明显闪烁抖动。

## 定位过程

1. **静态**：`pre-final` 是 OptiFine 内置 ShadersMod 的检查点字符串，位于
   `shadersmod.client.Shaders.checkGLError(String)`，由 `Shaders.renderFinal()` 调用
   （字节码位置：`renderFinal` 偏移 146）。所以 0x0500 是 ShadersMod 自己在 final 合成前
   抽到的真实 GL 错误。
2. **动态**：写了 Forge 探针模组 `GLDebugProbe`，注册 **同步** GL 调试回调
   （`KHR_debug`，NVIDIA 上可用；关键点是除了 `GL_DEBUG_OUTPUT_SYNCHRONOUS` 还必须
   `glEnable(GL_DEBUG_OUTPUT)`，否则一条消息都收不到）。同步回调在非法调用**当场**触发，
   打印出的 Java 栈顶就是元凶：

```
GL_INVALID_ENUM error generated. Framebuffer <target> is invalid.
    at org.lwjgl.opengl.GL30.glFramebufferTexture2D(GL30.java:1186)
    at net.minecraft.client.renderer.OpenGlHelper.func_153188_a(OpenGlHelper.java:693)
    at shadersmod.client.Shaders.renderFinal(Shaders.java:4729)      <-- pre-final 之前
    at shadersmod.client.Shaders.renderComposites(Shaders.java:4687)
    at shadersmod.client.Shaders.renderCompositeFinal(Shaders.java:4519)
    at net.minecraft.client.renderer.EntityRenderer.func_78471_a(EntityRenderer.java:1924)
```

3. **根因**：OptiFine 为 1.7.10 附带了一个 1.8 风格的 `GlStateManager` shim（默认包，
   `GlStateManager.class`）。它的静态初始化是这样复制常量的：

```java
static {
    GL_FRAMEBUFFER       = OpenGlHelper.<e>;   // buu.e
    GL_RENDERBUFFER      = OpenGlHelper.<f>;
    GL_COLOR_ATTACHMENT0 = OpenGlHelper.<g>;
    GL_DEPTH_ATTACHMENT  = OpenGlHelper.<h>;
}
```

   这组值由 `OpenGlHelper` 的静态初始化从 **GL 上下文能力**里取。OptiFine 的 `Shaders`
   在游戏早期（GL 上下文尚未创建、Display 还没建）就被加载，于是 `OpenGlHelper` 判定
   "不支持 framebuffer"，这四个 int 全部停在 **0**。

   结果每帧 `Shaders.renderFinal()` 都执行
   `glFramebufferTexture2D(0, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, <texture>, 0)`，
   `target=0` 不是合法枚举 → `GL_INVALID_ENUM`；MC 画面 framebuffer 的颜色附件永远贴不上
   final 合成纹理 → 闪烁。

   实测确认（探针输出）：`GL_FRAMEBUFFER=0 GL_RENDERBUFFER=0 GL_COLOR_ATTACHMENT0=0 GL_DEPTH_ATTACHMENT=0`。

   注意：这与 BSL 版本无关（BSL 的 `DRAWBUFFERS` 里用 8/9 号 colortex 被 1.7.10 拒收只是
   另一条独立告警，不是本错误的来源）。

## 修复

`GlStateManagerFix`：在 GL 上下文就绪后的第一个渲染帧，用反射把 shim 的四个字段改回
真正的 GL30 常量，并在前 240 帧重复施加以防 shim 被延迟初始化：

| 字段 | 修正值 |
|---|---|
| `GL_FRAMEBUFFER` | 36160 (`GL30.GL_FRAMEBUFFER`) |
| `GL_RENDERBUFFER` | 36161 |
| `GL_COLOR_ATTACHMENT0` | 36064 |
| `GL_DEPTH_ATTACHMENT` | 36096 |

实测结果：`GL error 0x0500` 计数 **5216 → 0**，闪烁消失。

模组源码：
- `E:\work\forge-1-7-10\src\main\java\com\example\forge1710\GLDebugProbe.java`（GL 调试回调 + 栈落盘）
- `E:\work\forge-1-7-10\src\main\java\com\example\forge1710\GlStateManagerFix.java`（修复本体）

产物：`E:\work\mc1710\mods\forge1710-0.1.0.jar`（`gradlew deployToInstance` 直接部署）。
诊断日志写在游戏目录下的 `gldebug-probe.log`。

## 备注

- 这是绕过 OptiFine 1.7.10 shim 早期类初始化缺陷的做法；根本修法是让 OptiFine 在建上下文
  之后再初始化 `GlStateManager`（上游问题）。只要用这个模组，任何 1.7.10 + OptiFine + 光影的
  组合都不再产生该错误。
- 探针默认在前 40 条唯一错误后停止输出，避免每帧刷屏；`GLDEBUG` 只记录
  `GL_DEBUG_TYPE_ERROR` 与 `UNDEFINED_BEHAVIOR`，过滤掉驱动噪声。

---

# 附加功能：OpenGL 报错静默（对齐 Open-GL-error-Suppressor-1.0）

参考实现：<https://github.com/Mable2015/Open-GL-error-Suppressor-1.0>
（`com.fix.openglsuppressor.OpenGLErrorSuppressor`，三个拦截器）。本模组按同样的三个拦截点实现，
但**不是静默黑洞**。

## 拦截器 1：聊天栏过滤 —— `ChatSpamFilter`

`ClientChatReceivedEvent` 在 1.7.10 上是 `@Cancelable`，命中规则直接 `setCanceled(true)`，
消息在进聊天 GUI 之前就被丢掉。规则可改：`config/forge1710-chatfilter.txt`（每行一个正则，`#` 注释）。

内置规则（9 条）覆盖 `OpenGL 128x`、`GL error 0x____`、`Invalid enum at`、`GL_INVALID_*`、
`at pre-final`、`########## GL ERROR`、以及"含 GL/OpenGL/shader 且含 1283 或 stack overflow"的行。

## 拦截器 2 + 3：提前 drain GL 错误 —— `GLErrorSuppressor`

Minecraft 1.7.10 自己（`bao` 类里 `########## GL ERROR ##########` / `@ ` / `Pre render` /
`preRenderErrors` 两个字符串）在 runTick 末尾轮询 `glGetError()`，所以只要**在那一轮之前**把挂起的
错误读掉，banner 就永远不会打印：

- `TickEvent.RenderTickEvent` **Phase.END**（每帧渲染结束）
- `GuiScreenEvent.DrawScreenEvent.Post`（每个 GUI 界面绘制完成，背包/容器就是这里出 1283）

drain 上限 128 次/调用，防死循环。

**与参考实现的差异（重要）**：不是把错误直接丢掉就算了——
- 每个被吞掉的错误码都会计入统计，首次出现时写入 `gldebug-probe.log`；
- `GLDEBUG` 的同步 KHR_debug 回调不受 drain 影响，仍会记录每个真实错误的调用栈；
- 只有 `1283 (GL_STACK_OVERFLOW)` 和 `65539 (0x10003)` 被标记为 "ignorable"，其他码会明确写出
  `(NOT in the ignorable set - check gldebug-probe.log)`，避免掩盖真实 bug。

开关：`config/forge1710-suppress.txt`（`suppressGL=true/false`），或 `-Dforge1710.suppressGL=false`。
`selfTest=true` 是开发用开关：故意制造一个 GL 错误，验证 drain 是否在 banner 之前吃掉它。

## 验证记录

| 项 | 结果 |
|---|---|
| 聊天过滤自检 | 合成 `OpenGL 1283 error at pre-final` → `canceled=true`，计数 +1 |
| drain 端到端自检 | 注入 `GL_INVALID_ENUM` → 记录 `first 1280 (GL_INVALID_ENUM) drained at gui-draw-post` |
| banner 抑制 | 控制台 `########## GL ERROR` = **0** 行，`GL error 0x` = **0** 行 |
| 渲染 | BSL 正常，无 0x0500，画面不闪 |

