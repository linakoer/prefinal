# Store page text — English (machine translation)

> **Machine translation notice**
> This English text is **machine-translated** from [store-description.md](store-description.md)
> and is provided for convenience only. The Chinese text is authoritative.

## Summary

```
Fixes "GL error 0x0500: Invalid enum at pre-final" on Minecraft 1.7.10 + OptiFine: the error no longer refreshes every frame and the log stays clean.
```

## Description

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
