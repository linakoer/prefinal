package com.linakoer.prefinal;

import java.lang.reflect.Field;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

import org.lwjgl.opengl.GL30;

/**
 * Repairs OptiFine's bundled GlStateManager shim (default package, loaded from the OptiFine jar).
 *
 * The shim's static initialiser copies the framebuffer enums out of OpenGlHelper:
 *     GL_FRAMEBUFFER       = OpenGlHelper.<e>
 *     GL_RENDERBUFFER      = OpenGlHelper.<f>
 *     GL_COLOR_ATTACHMENT0 = OpenGlHelper.<g>
 *     GL_DEPTH_ATTACHMENT  = OpenGlHelper.<h>
 * OpenGlHelper fills those from the GL context capabilities. When the shim is initialised before a
 * GL context exists (OptiFine's Shaders class is touched during early loading), OpenGlHelper reports
 * "no framebuffer support" and leaves all four at 0. From then on every
 * Shaders.renderFinal() -> OpenGlHelper.glFramebufferTexture2D(GlStateManager.GL_FRAMEBUFFER, ...)
 * call passes target = 0, the driver raises GL_INVALID_ENUM ("Framebuffer <target> is invalid"),
 * the final composite never lands on the screen framebuffer and the picture flickers.
 *
 * Once a context is current we rewrite the four fields with the real GL30 values. The rewrite is
 * repeated for the first frames in case the shim is initialised lazily, after our first tick.
 */
public final class GlStateManagerFix {

    private static final int REPATCH_FRAMES = 240;

    private static int frames;
    private static boolean resolved;
    private static Class<?> shim;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase != Phase.START || frames >= REPATCH_FRAMES) {
            return;
        }
        frames++;
        try {
            if (shim == null) {
                shim = Class.forName("GlStateManager");
                log("shim class found: " + shim.getName());
            }
            if (!resolved) {
                logValues("before");
                resolved = true;
            }
            patch("GL_FRAMEBUFFER", GL30.GL_FRAMEBUFFER);
            patch("GL_RENDERBUFFER", GL30.GL_RENDERBUFFER);
            patch("GL_COLOR_ATTACHMENT0", GL30.GL_COLOR_ATTACHMENT0);
            patch("GL_DEPTH_ATTACHMENT", GL30.GL_DEPTH_ATTACHMENT);
            if (frames == 1 || frames % 60 == 0) {
                logValues("frame " + frames);
            }
        } catch (Throwable t) {
            log("patch failed: " + t);
            t.printStackTrace();
        }
    }

    private static void patch(String name, int expected) throws Exception {
        Field f = shim.getField(name);
        if (f.getInt(null) != expected) {
            f.setInt(null, expected);
            log("patched " + name + " -> " + expected);
        }
    }

    private static void logValues(String when) throws Exception {
        StringBuilder sb = new StringBuilder("[GLFIX] ").append(when).append(": ");
        for (String n : new String[] { "GL_FRAMEBUFFER", "GL_RENDERBUFFER", "GL_COLOR_ATTACHMENT0", "GL_DEPTH_ATTACHMENT" }) {
            sb.append(n).append('=').append(shim.getField(n).getInt(null)).append(' ');
        }
        log(sb.toString().trim());
    }

    private static void log(String s) {
        System.err.println(s);
        GLDebugProbe.writeExternal(s);
    }
}
