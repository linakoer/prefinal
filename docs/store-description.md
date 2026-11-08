# 商店页面文案（CurseForge / Modrinth 直接用）

> 直接把下面「摘要」和「正文」贴到对应输入框即可。英文版见
> [store-description.en.md](store-description.en.md)（机器翻译，中文为准）。

## 摘要 / Summary（一行，两个平台都用）

```
修复 Minecraft 1.7.10 + OptiFine 的 "GL error 0x0500: Invalid enum at pre-final"：画面不再闪烁，日志不再刷屏。
```

英文摘要（投英文区时用）：

```
Fixes "GL error 0x0500: Invalid enum at pre-final" on Minecraft 1.7.10 + OptiFine: no more flickering picture and log spam.
```

---

## 正文 / Description（Markdown）

# PreFinal

> **适用声明**
> 本模组**主要修复最新版 BSL 光影在 OptiFine 上使用时**产生的
> `GL error 0x0500: Invalid enum at pre-final` 错误。
> 其他支持 1.7.10 OptiFine 的现代光影包，只要出现**同一个报错**，同样可以用本模组修复。
>
> 已测试组合：**BSL v10 + OptiFine HD U E7 + FalseTweaks 4.4.5** —— 不再报错。

修复 1.7.10 + OptiFine 客户端上 `GL error 0x0500: Invalid enum at pre-final` 造成的画面闪烁与日志刷屏。

## 现象

装了 OptiFine HD U E7 和现代光影包（实测 BSL Classic v10）后进入世界：

- 日志每帧刷 `GL error 0x0500: Invalid enum at pre-final`（实测 40 秒 5216 条）；
- 画面明显闪烁抖动。

## 原因

OptiFine 自带一个 1.8 风格的 `GlStateManager` shim，它在**静态初始化**时从 `OpenGlHelper` 复制四个
framebuffer 枚举。但 OptiFine 的 `Shaders` 类在 GL 上下文创建之前就被加载，于是 `OpenGlHelper`
判定"不支持 framebuffer"，这四个值全部停在 **0**。

之后每帧 `Shaders.renderFinal()` 都会执行：

```java
glFramebufferTexture2D(0 /* 不是 GL_FRAMEBUFFER */, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

产生 `GL_INVALID_ENUM`，final 合成永远盖不到屏幕 framebuffer 上 —— 这就是闪烁的来源。
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

## 许可与致谢

MIT 协议。不使用、不打包任何 OptiFine / 游戏 / 其它模组的代码或资源；只在你本地运行时修正
OptiFine 的四个静态字段。感谢 OptiFine 作者与光影作者们的长期维护。

> 英文版为机器翻译，仅供参考；以本中文说明为准。
