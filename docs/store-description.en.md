# Store page text — English (machine translation)

> **Machine translation notice**
> This English text is **machine-translated** from [store-description.md](store-description.md)
> and is provided for convenience only. The Chinese text is authoritative.

## Summary

```
Fixes "GL error 0x0500: Invalid enum at pre-final" on Minecraft 1.7.10 + OptiFine: no more flickering picture and log spam.
```

## Description

# PreFinal

Fixes the picture flicker and log spam caused by `GL error 0x0500: Invalid enum at pre-final` on
Minecraft 1.7.10 + OptiFine clients.

## Symptom

With OptiFine HD U E7 and a modern shaderpack (tested with BSL Classic v10), entering a world:

- floods the log with `GL error 0x0500: Invalid enum at pre-final` (5216 lines in 40 seconds);
- makes the picture visibly flicker and jitter.

## Cause

OptiFine ships a 1.8-style `GlStateManager` shim that copies four framebuffer enums out of
`OpenGlHelper` in its **static initialiser**. OptiFine's `Shaders` class is loaded before a GL
context exists, so `OpenGlHelper` reports "no framebuffer support" and all four values stay **0**.

Every frame afterwards, `Shaders.renderFinal()` runs:

```java
glFramebufferTexture2D(0 /* not GL_FRAMEBUFFER */, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

which raises `GL_INVALID_ENUM`, so the final composite never reaches the screen framebuffer — that
is the flicker. **Not shaderpack specific, not a driver problem.**

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

## Licence

MIT. No OptiFine / game / mod code or assets are bundled; the mod only corrects four static fields
of OptiFine at runtime on your own machine.
