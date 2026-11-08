package com.linakoer.prefinal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Cancels OpenGL noise that would otherwise be written into the chat bar.
 *
 * ClientChatReceivedEvent is @Cancelable in 1.7.10, so setting canceled drops the line before the
 * chat GUI ever shows it.
 *
 * Patterns come from [errors] chatFilterPatterns in prefinal.cfg; when that list is empty the
 * built-in set below is used. Toggle the whole component with [errors] enableChatFilter.
 */
public final class ChatSpamFilter {

    private static final String[] DEFAULTS = {
        // "OpenGL 1283 error", "OpenGL1283ERROR", "OpenGL 1281"
        "(?i)opengl\\s*128\\d",
        // "GL error 0x0500: Invalid enum at pre-final"
        "(?i)gl\\s*error\\s*0x[0-9a-f]{4}",
        "(?i)invalid\\s+enum\\s+at\\s+",
        "(?i)\\bGL_INVALID_(ENUM|VALUE|OPERATION|FRAMEBUFFER_OPERATION)\\b",
        "(?i)\\bat\\s+pre-final\\b",
        // Minecraft's own banner: "########## GL ERROR ########## @ Pre render"
        "(?i)##########\\s*GL\\s*ERROR",
        // 1283 stack overflow, but only when the line is clearly about GL
        "(?i)(opengl|\\bgl\\b|shader).{0,40}(stack\\s*overflow|1283)",
        // OptiFine / ShadersMod failure banners
        "(?i)\\bfailed\\s+to\\s+load\\s+shader",
        "(?i)\\bshaderpack\\b.*\\b(error|failed)\\b"
    };

    private static List<Pattern> patterns;
    private static long filtered;
    private static boolean selfTested;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) {
            return;
        }
        String text = safeText(event.message);
        if (isSpam(text)) {
            event.setCanceled(true);
            filtered++;
            if (filtered == 1 || filtered % 100 == 0) {
                PreFinal.log("chat filter: suppressed " + filtered + " message(s), latest: " + text);
            }
        }
    }

    /** Runs once when [debug] selfTest is on: proves the filter cancels a matching event. */
    public void selfTest() {
        if (selfTested) {
            return;
        }
        selfTested = true;
        try {
            IChatComponent probe = new ChatComponentText("OpenGL 1283 error at pre-final [chatfilter self-test]");
            ClientChatReceivedEvent test = new ClientChatReceivedEvent(probe);
            MinecraftForge.EVENT_BUS.post(test);
            PreFinal.log("chat filter self-test: canceled=" + test.isCanceled());
        } catch (Throwable t) {
            PreFinal.log("chat filter self-test failed: " + t);
        }
    }

    private static boolean isSpam(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (Pattern p : patterns()) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static synchronized List<Pattern> patterns() {
        if (patterns != null) {
            return patterns;
        }
        List<Pattern> list = new ArrayList<Pattern>();
        String[] configured = PreFinalConfig.chatFilterPatterns;
        if (configured != null && configured.length > 0) {
            for (String line : configured) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                try {
                    list.add(Pattern.compile(line.trim()));
                } catch (Throwable bad) {
                    PreFinal.log("bad chat filter pattern ignored: " + line);
                }
            }
        }
        if (list.isEmpty()) {
            for (String s : DEFAULTS) {
                list.add(Pattern.compile(s));
            }
        }
        PreFinal.log("chat filter: " + list.size() + " pattern(s) active ("
                + (configured != null && configured.length > 0 ? "from prefinal.cfg" : "built-in set") + ")");
        patterns = list;
        return patterns;
    }

    private static String safeText(IChatComponent c) {
        try {
            return c.getUnformattedText();
        } catch (Throwable t) {
            return String.valueOf(c);
        }
    }
}
