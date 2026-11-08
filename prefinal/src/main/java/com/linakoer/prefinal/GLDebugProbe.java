package com.linakoer.prefinal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.opengl.KHRDebugCallback;

/**
 * Installs a synchronous GL debug callback (KHR_debug, else ARB_debug_output) and dumps the
 * Java stack of every real GL error the driver raises. Because the callback is synchronous the
 * stack top is the offending GL call itself - this pins down "GL error 0x0500: Invalid enum
 * at pre-final" to the exact caller inside OptiFine's ShadersMod.
 *
 * Client thread only: installed from the first RenderTickEvent START, where the GL context is current.
 * Output goes to gldebug-probe.log in the game directory (the console is flooded every frame).
 */
public final class GLDebugProbe {

    private static final int MAX_UNIQUE_REPORTS = 40;

    private static boolean installed;
    private static final Set<String> seen = new HashSet<String>();
    private static int printed;
    private static PrintStream file;

    private static PrintStream file() {
        if (file == null) {
            try {
                file = new PrintStream(new FileOutputStream(new File("gldebug-probe.log"), true), true);
            } catch (Throwable t) {
                file = System.err;
            }
        }
        return file;
    }

    private static void out(String s) {
        System.err.println(s);
        file().println(s);
    }

    /** Lets the other diagnostic classes share the same log file. */
    public static void writeExternal(String s) {
        try {
            file().println(s);
        } catch (Throwable ignored) {
            // logging must never break the game
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (installed || event.phase != Phase.START) {
            return;
        }
        installed = true;
        try {
            ContextCapabilities caps = GLContext.getCapabilities();
            if (caps.GL_KHR_debug) {
                KHRDebug.glDebugMessageCallback(new KHRDebugCallback(new KHRDebugCallback.Handler() {
                    public void handleMessage(int source, int type, int id, int severity, String message) {
                        report(source, type, id, severity, message);
                    }
                }));
                // KHR_debug needs GL_DEBUG_OUTPUT enabled as well, otherwise no messages are
                // generated at all; SYNCHRONOUS makes the callback fire inside the offending call.
                GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT);
                GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
                out("[GLDEBUG] installed via KHR_debug, GL_VERSION="
                        + GL11.glGetString(GL11.GL_VERSION)
                        + " GL_RENDERER=" + GL11.glGetString(GL11.GL_RENDERER));
            } else if (caps.GL_ARB_debug_output) {
                ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new ARBDebugOutputCallback.Handler() {
                    public void handleMessage(int source, int type, int id, int severity, String message) {
                        report(source, type, id, severity, message);
                    }
                }));
                GL11.glEnable(ARBDebugOutput.GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB);
                out("[GLDEBUG] installed via ARB_debug_output, GL_VERSION="
                        + GL11.glGetString(GL11.GL_VERSION));
            } else {
                out("[GLDEBUG] neither KHR_debug nor ARB_debug_output is available");
            }
        } catch (Throwable t) {
            out("[GLDEBUG] install failed: " + t);
            t.printStackTrace();
        }
    }

    private static void report(int source, int type, int id, int severity, String message) {
        // keep only genuine errors; notifications, performance and deprecation hints are noise here
        if (type != KHRDebug.GL_DEBUG_TYPE_ERROR && type != KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR) {
            return;
        }
        String stack = renderStack();
        String key = message + "#" + stack;
        if (!seen.add(key)) {
            return;
        }
        if (printed >= MAX_UNIQUE_REPORTS) {
            return;
        }
        printed++;
        out("[GLDEBUG] ---------- unique error " + printed + "/" + MAX_UNIQUE_REPORTS + " ----------");
        out("[GLDEBUG] message : " + message);
        out("[GLDEBUG] source=" + source + " type=" + type + " id=" + id + " severity=" + severity);
        file().print(stack);
        System.err.print(stack);
    }

    private static String renderStack() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < st.length && i < 40; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            if (cls.equals("java.lang.Thread") || cls.equals(GLDebugProbe.class.getName())) {
                continue;
            }
            sb.append("    at ").append(cls).append('.').append(e.getMethodName())
              .append('(').append(e.getFileName()).append(':').append(e.getLineNumber()).append(")\n");
        }
        return sb.toString();
    }
}
