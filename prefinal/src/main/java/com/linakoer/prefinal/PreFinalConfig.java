package com.linakoer.prefinal;

import java.io.File;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import net.minecraftforge.common.config.Configuration;

/**
 * Single configuration file for PreFinal: config/prefinal.cfg
 *
 * The file is generated with comments on first start and can be edited in-game via the Mods list
 * (Forge config GUI) or by hand. Everything can be switched off independently, because the mod is
 * a repair kit: if one part misbehaves on a particular pack, the others still work.
 */
public final class PreFinalConfig {

    public static final String FILE_NAME = "prefinal.cfg";

    private static Configuration cfg;

    // [fix]
    public static boolean enableGlStateManagerFix = true;

    // [errors]
    public static boolean enableErrorSuppression = true;
    public static boolean enableChatFilter = true;
    public static String[] chatFilterPatterns = new String[0];

    // [debug]
    public static boolean enableGlDebugProbe = false;
    public static boolean selfTest = false;

    private PreFinalConfig() {
    }

    public static void load(FMLPreInitializationEvent event) {
        File file = event.getSuggestedConfigurationFile();
        cfg = new Configuration(file);
        try {
            cfg.load();
            read();
        } catch (Throwable t) {
            PreFinal.log("config load failed, using defaults: " + t);
        } finally {
            if (cfg != null) {
                try {
                    if (cfg.hasChanged()) {
                        cfg.save();
                    }
                } catch (Throwable ignored) {
                    // a read-only config must never break the game
                }
            }
        }
        PreFinal.log("config: fix=" + enableGlStateManagerFix
                + " errorSuppression=" + enableErrorSuppression
                + " chatFilter=" + enableChatFilter
                + (chatFilterPatterns.length > 0 ? " (custom patterns: " + chatFilterPatterns.length + ")" : " (built-in patterns)")
                + " debugProbe=" + enableGlDebugProbe
                + " selfTest=" + selfTest);
    }

    private static void read() {
        enableGlStateManagerFix = cfg.getBoolean(
                "enableGlStateManagerFix", "fix", true,
                "The actual repair for 'GL error 0x0500: Invalid enum at pre-final'.\n"
                        + "Restores the four framebuffer enums that OptiFine's GlStateManager shim\n"
                        + "left at 0 because it initialised before a GL context existed.\n"
                        + "Disable only to confirm the bug comes back.");

        enableErrorSuppression = cfg.getBoolean(
                "enableErrorSuppression", "errors", true,
                "Drain pending GL errors at the end of every rendered frame and after each GUI draw,\n"
                + "so Minecraft's own glGetError() poll stops printing\n"
                + "'########## GL ERROR ########## @ Pre render' and the 1283 GUI spam.\n"
                + "Drained codes are counted in gldebug-probe.log; nothing is silently lost.");

        enableChatFilter = cfg.getBoolean(
                "enableChatFilter", "errors", true,
                "Cancel chat lines that are OpenGL noise (error text sent to the chat bar).");

        chatFilterPatterns = cfg.getStringList(
                "chatFilterPatterns", "errors", new String[0],
                "Optional regex list, one pattern per line. Leave empty to use the built-in set\n"
                        + "(OpenGL 128x, 'GL error 0xNNNN', 'Invalid enum at', '########## GL ERROR', ...).\n"
                        + "Example:\n"
                        + "    chatFilterPatterns <\n"
                        + "        (?i)opengl\\s*128\\d\n"
                        + "        (?i)gl\\s*error\\s*0x[0-9a-f]{4}\n"
                        + "    >");

        enableGlDebugProbe = cfg.getBoolean(
                "enableGlDebugProbe", "debug", false,
                "Install a synchronous KHR_debug callback and write the real call stack of every\n"
                + "driver-raised GL error to gldebug-probe.log. Off by default; turn it on when\n"
                + "diagnosing a new GL problem.");

        selfTest = cfg.getBoolean(
                "selfTest", "debug", false,
                "Raise one deliberate GL_INVALID_ENUM shortly after start and verify that the error\n"
                + "suppressor eats it before the banner can print. Developer switch.");
    }

    /** Re-read the values after the in-game config GUI changed them. */
    public static void reload() {
        if (cfg == null) {
            return;
        }
        try {
            cfg.load();
            read();
        } catch (Throwable t) {
            PreFinal.log("config reload failed: " + t);
        }
    }
}
