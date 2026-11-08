package com.linakoer.prefinal;

import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.opengl.GL11;

/**
 * Keeps the OpenGL error noise out of the log and the chat.
 *
 * Minecraft 1.7.10 polls glGetError() at the end of runTick (class bao, strings
 * "########## GL ERROR ##########", "Pre render", "preRenderErrors") and dumps three lines per
 * pending error. Draining the queue before that poll runs means the banner never prints.
 *
 * This is NOT a black hole: every drained code is counted, the first occurrence of each code is
 * written to gldebug-probe.log, and codes outside the ignorable set are flagged explicitly so a
 * genuine problem cannot disappear silently. Enable [debug] enableGlDebugProbe in prefinal.cfg to
 * additionally capture the real Java call stack of every error.
 *
 * Toggle the whole component with [errors] enableErrorSuppression in prefinal.cfg.
 */
public final class GLErrorSuppressor {

    /** 1283 = GL_STACK_OVERFLOW (the GUI/inventory case), 65539 = 0x10003 seen in the same banner. */
    private static final int[] IGNORABLE = { 1283, 65539 };

    private static final int MAX_DRAIN_PER_CALL = 128;

    private static final Map<Integer, Integer> drained = new LinkedHashMap<Integer, Integer>();
    private static boolean selfTestDone;
    private static int passes;
    private static long total;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            drain("render-tick-end");
        }
    }

    @SubscribeEvent
    public void onGuiScreenDrawn(GuiScreenEvent.DrawScreenEvent.Post event) {
        drain("gui-draw-post");
    }

    private void drain(String where) {
        try {
            passes++;
            drainOnce(where);

            // one-shot end-to-end check from [debug] selfTest
            if (PreFinalConfig.selfTest && !selfTestDone && passes > 300) {
                selfTestDone = true;
                PreFinal.log("self-test: injecting GL_INVALID_ENUM");
                GL11.glEnable(0); // invalid enum on purpose
                drainOnce(where);
            }
        } catch (Throwable t) {
            // never let error suppression break rendering
        }
    }

    private static void drainOnce(String where) {
        int drainedNow = 0;
        int code;
        while ((code = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            record(code, where);
            drainedNow++;
            if (drainedNow >= MAX_DRAIN_PER_CALL) {
                PreFinal.log("hit drain cap (" + MAX_DRAIN_PER_CALL + ") at " + where + ", stopping this pass");
                break;
            }
        }
    }

    private static void record(int code, String where) {
        total++;
        Integer previous = drained.get(code);
        drained.put(code, previous == null ? 1 : previous + 1);
        if (previous == null) {
            PreFinal.log("first " + describe(code) + " drained at " + where
                    + (isIgnorable(code) ? " (ignorable)" : " (NOT in the ignorable set - check gldebug-probe.log)"));
        } else if (total % 500 == 0) {
            PreFinal.log("drained " + total + " GL error(s) so far: " + summary());
        }
    }

    private static String summary() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : drained.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(describe(e.getKey())).append('x').append(e.getValue());
        }
        return sb.toString();
    }

    private static String describe(int code) {
        String name;
        switch (code) {
            case 1280: name = "GL_INVALID_ENUM"; break;
            case 1281: name = "GL_INVALID_VALUE"; break;
            case 1282: name = "GL_INVALID_OPERATION"; break;
            case 1283: name = "GL_STACK_OVERFLOW"; break;
            case 1284: name = "GL_STACK_UNDERFLOW"; break;
            case 1285: name = "GL_OUT_OF_MEMORY"; break;
            case 1286: name = "GL_INVALID_FRAMEBUFFER_OPERATION"; break;
            default: name = "unknown"; break;
        }
        return code + " (" + name + ")";
    }

    private static boolean isIgnorable(int code) {
        for (int i : IGNORABLE) {
            if (i == code) {
                return true;
            }
        }
        return false;
    }
}
