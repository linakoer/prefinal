# PreFinal

> **Machine translation notice**
> This English document is **machine-translated** from the Chinese original
> ([README.md](README.md)) and is provided for convenience only. The Chinese version is
> authoritative; if anything here is unclear or contradicts it, follow the Chinese text.

> **Scope**
> This mod **primarily fixes `GL error 0x0500: Invalid enum at pre-final`** produced by **current
> BSL shaders used with OptiFine**.
> Any other modern shaderpack for 1.7.10 OptiFine that produces **the same error** can be repaired
> by this mod as well.
>
> Tested with: **BSL v10 + OptiFine HD U E7 + FalseTweaks 4.4.5** — no more errors.

Fixes the shader error on **Minecraft 1.7.10 + OptiFine**

```
GL error 0x0500: Invalid enum at pre-final
```

* Project / mod name: **PreFinal** (mod id `prefinal`)
* Author: **linakoer** · version **1.0** · MIT licence
* Target: Minecraft 1.7.10 + Forge 10.13.4.1614, client side

Details and root-cause analysis: [prefinal/README.en.md](prefinal/README.en.md) ·
[docs/FINDINGS.md](docs/FINDINGS.md)

## Quick start

```
cd prefinal
gradlew.bat build              # -> build/libs/prefinal-1.0.jar
gradlew.bat deployToInstance   # copies into E:/work/mc1710/mods when present, else ./deploy
```

**JDK 8** is required (1.7.10 line: Gradle 7.4.2 + ForgeGradle 1.2 fork).

## Switches

Every component can be turned off independently in `config/prefinal.cfg`:

| Switch | Effect |
|---|---|
| `fix.enableGlStateManagerFix` | the 0x0500 repair itself |
| `errors.enableErrorSuppression` | drain GL errors so the banner / 1283 spam never prints |
| `errors.enableChatFilter` | filter OpenGL noise out of the chat bar |
| `errors.chatFilterPatterns` | custom regex list, empty = built-in set |
| `debug.enableGlDebugProbe` | record real GL error call stacks |
| `debug.selfTest` | inject one GL error to verify the suppression |

## Publishing

| Purpose | File |
|---|---|
| Changelog | [CHANGELOG.md](CHANGELOG.md) |
| Store page text (Chinese, authoritative) | [docs/store-description.md](docs/store-description.md) |
| Store page text (English, machine translation) | [docs/store-description.en.md](docs/store-description.en.md) |
| Upload steps and field mapping | [docs/upload-checklist.md](docs/upload-checklist.md) |
| Icons | `artworks/icon-512.png`, `artworks/icon-1024.png` |

## Out of scope (explicit)

This mod **only** repairs GL error 0x0500: Invalid enum at pre-final caused by OptiFine's
GlStateManager initialisation order, plus the log/chat spam it produces. Not fixed here:

* **BSL Classic** on OptiFine: stretched/misplaced shadows and jitter - use the **non-Classic** BSL v10.
* **Artifacts caused by the shaderpack itself** not matching OptiFine: misplaced geometry, flicker,
  wrong shadows, shader compile failures.
* **Third-party mod conflicts** such as Shader Fixer vs JourneyMap version mismatch crashing startup.

**Rule of thumb: anything that goes away by switching the shaderpack or a mod version is out of scope.**