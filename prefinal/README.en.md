# PreFinal

>   Machine translation notice  
> This English document is   machine-translated   from the Chinese original
> ([README.md](README.md)) and is provided for convenience only. The Chinese version is
> authoritative; if anything here is unclear or contradicts it, follow the Chinese text.

A Minecraft   1.7.10 / Forge 10.13.4.1614   client mod that fixes the shader error

```
GL error 0x0500: Invalid enum at pre-final
```

Author   linakoer   · version   1.0   · mod id `prefinal` · MIT licence

## What goes wrong

On a 1.7.10 client with   OptiFine HD U E7   and a modern shaderpack (tested with BSL Classic v10),
entering a world causes:

  the log to flood with `GL error 0x0500: Invalid enum at pre-final` (5216 lines in 40 seconds).

  Root cause   (established with a synchronous GL debug callback; details in
[docs/FINDINGS.md](docs/FINDINGS.md)): OptiFine ships a 1.8-style `GlStateManager` shim whose static
initialiser copies four framebuffer enums out of `OpenGlHelper`. OptiFine's `Shaders` class is
touched before a GL context exists, so `OpenGlHelper` reports "no framebuffer support" and all four
values stay   0  . From then on every frame `Shaders.renderFinal()` calls

```java
glFramebufferTexture2D(0 /  not GL_FRAMEBUFFER  /, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
```

which raises `GL_INVALID_ENUM`, and the final composite never lands on the screen framebuffer.

This is neither shaderpack specific nor a driver problem.

## What the mod does

| Component | Purpose |
|---|---|
| `GlStateManagerFix` |   the actual fix   — once a GL context is current, restores `GL_FRAMEBUFFER` / `GL_RENDERBUFFER` / `GL_COLOR_ATTACHMENT0` / `GL_DEPTH_ATTACHMENT` on OptiFine's shim to their real GL values (36160 / 36161 / 36064 / 36096), re-applied for the first frames in case the shim is initialised lazily |
| `GLErrorSuppressor` | keeps Minecraft's own `runTick` poll from printing `########## GL ERROR ########## @ Pre render` and the 1283 GUI spam; drained codes are counted and logged, never silently dropped |
| `ChatSpamFilter` | cancels OpenGL noise text that would otherwise appear in the chat bar (configurable regexes) |
| `GLDebugProbe` | optional diagnostic: a synchronous `KHR_debug` callback that writes the real call stack of any driver error to `gldebug-probe.log` |

The first row is why this mod exists; the rest is incidental, so that one failure does not bury the
log and the chat.

## Install

Drop `prefinal-1.0.jar` into `mods/` of a Forge 1.7.10 client. Client side only
(`acceptableRemoteVersions = " "`, harmless on a server).

## Config

One generated file:   `config/prefinal.cfg`   (created on first start with comments; editable
in-game through Forge's mod config list).

```
fix {
    B:enableGlStateManagerFix=true       # the 0x0500 repair itself - turn it off to see the error return
}

errors {
    B:enableErrorSuppression=true        # drain GL errors so the banner / 1283 spam never prints
    B:enableChatFilter=true              # filter OpenGL noise out of the chat bar
    S:chatFilterPatterns <               # optional regex list, empty = built-in set of 9
     >
}

debug {
    B:enableGlDebugProbe=false           # KHR_debug call stacks -> gldebug-probe.log
    B:selfTest=false                     # inject one GL error to prove the suppression works
}
```

Each component is independent and can be switched off alone. The active combination is printed at
startup:

```
[PreFinal] config: fix=true errorSuppression=true chatFilter=true (built-in patterns) debugProbe=false selfTest=false
```

Changes take effect after a   game restart  .

## Build

JDK 8 required (1.7.10 line: Gradle 7.4.2 + ForgeGradle 1.2 fork).

```
gradlew.bat build
gradlew.bat deployToInstance    # copies into E:/work/mc1710/mods when present, else ./deploy
```

`gradle.properties` pins `org.gradle.java.home`; point it at your own JDK 8.

## Verified

Environment: MC 1.7.10 + Forge 10.13.4.1614 + OptiFine HD U E7 + BSL Classic v10, in a 73-mod pack.

| Metric | Result |
|---|---|
| `GL error 0x0500` lines | 5216 in 40 s →   0   |
| `########## GL ERROR` banner |   0   |
| Shader programs compiled | 29 / 29, picture stable |

## Out of scope (explicit)

This mod **only** repairs `GL error 0x0500: Invalid enum at pre-final` caused by OptiFine's
`GlStateManager` initialisation order, plus the log/chat spam it produces. The following are **not
fixed and will not be fixed** here - report them to the shaderpack or mod authors instead:

* **BSL Classic** on OptiFine: **stretched/misplaced shadows and jitter**. Classic is tuned for old
  shadersmod conventions and does not match OptiFine's pipeline; use the **non-Classic** BSL v10.
* **Any artifact caused by the shaderpack itself** (version, settings, built-in options) not
  matching OptiFine, e.g. misplaced geometry, flicker, wrong shadows, shader compile failures,
  option parsing warnings.
* **Third-party mod conflicts**, e.g. Shader Fixer vs JourneyMap version mismatch crashing startup.

In short: **anything that goes away by switching the shaderpack or a mod version is out of scope.**

## Licence

MIT — see [LICENSE](../LICENSE). Third-party interaction is documented in [NOTICE.md](../NOTICE.md);
no game or mod assets are redistributed by this repository.
