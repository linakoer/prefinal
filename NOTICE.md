# NOTICE — third-party components and licensing

**PreFinal** (mod id `prefinal`, author linakoer) is released under the MIT license (see LICENSE).
It contains **no third-party code** and ships **no third-party assets**. This file records what it
interacts with and why that is compatible with the MIT license.

## Runtime dependencies (not bundled, not modified, not redistributed)

| Component | Role | Notes |
|---|---|---|
| Minecraft 1.7.10 (Mojang) | target game | never redistributed; the user must own the game |
| Forge 10.13.4.1614 | mod loader / API | pulled by ForgeGradle at build time |
| OptiFine HD U E7 | the bug that is repaired | PreFinal inspects OptiFine's own classes at runtime via reflection; no OptiFine code is copied or shipped |

PreFinal has no compile-time dependency on any other mod.

## What PreFinal touches at runtime

* Four `public static int` fields of OptiFine's bundled `GlStateManager` shim
  (`GL_FRAMEBUFFER`, `GL_RENDERBUFFER`, `GL_COLOR_ATTACHMENT0`, `GL_DEPTH_ATTACHMENT`), which the
  shim left at 0 because it was initialised before a GL context existed. They are set to the real
  GL values so OptiFine's own `Shaders.renderFinal()` stops issuing an invalid GL call.
* The effect of Minecraft's per-frame `glGetError()` poll: pending errors are read and recorded
  before the game prints its `########## GL ERROR ##########` banner.
* Chat messages matching the configured OpenGL noise patterns are cancelled.

None of this is a redistribution of OptiFine; all of it is necessary for interoperability with the
installed build.

## Reverse-engineering note

The root cause was established by inspecting the shipped OptiFine jar with `javap` (checking the
`GlStateManager` static initialiser and `Shaders.renderFinal`) and by observing GL behaviour at
runtime through a synchronous `KHR_debug` callback. Both are interoperability work; no decompiled
OptiFine source is included here.

## Game and mod assets

No Minecraft, OptiFine or other mod assets are committed. `tools/build-texturepack.ps1` (a
convenience script for unrelated missing-texture warnings) regenerates any such pack **on the
user's own machine** from assets the user already has installed; the generated pack is excluded
from version control because recolored vanilla textures remain subject to the Minecraft EULA and
mod textures remain subject to their authors' licenses.
