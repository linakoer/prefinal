package com.linakoer.prefinal;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

import net.minecraftforge.common.MinecraftForge;

/**
 * PreFinal - repairs "GL error 0x0500: Invalid enum at pre-final" on Minecraft 1.7.10 + OptiFine.
 *
 * Every part can be switched off in config/prefinal.cfg, because on a heavily modded pack one
 * component may need to stay out of the way while the others keep working.
 */
@Mod(modid = PreFinal.MODID, name = "PreFinal", version = "1.0",
     acceptableRemoteVersions = "*")
public class PreFinal {

    public static final String MODID = "prefinal";

    public static void log(String s) {
        System.err.println("[PreFinal] " + s);
        GLDebugProbe.writeExternal("[PreFinal] " + s);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PreFinalConfig.load(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }
        registerClient();
    }

    private void registerClient() {
        // [fix] the actual repair - OptiFine's GlStateManager kept its framebuffer enums at 0.
        if (PreFinalConfig.enableGlStateManagerFix) {
            GlStateManagerFix fix = new GlStateManagerFix();
            FMLCommonHandler.instance().bus().register(fix);
            MinecraftForge.EVENT_BUS.register(fix);
            log("GlStateManager fix   : enabled");
        } else {
            log("GlStateManager fix   : DISABLED (0x0500 will return)");
        }

        // [errors] keep pending GL errors away from Minecraft's own per-frame poll.
        if (PreFinalConfig.enableErrorSuppression) {
            GLErrorSuppressor suppressor = new GLErrorSuppressor();
            FMLCommonHandler.instance().bus().register(suppressor);
            MinecraftForge.EVENT_BUS.register(suppressor);
            log("GL error suppression : enabled");
        } else {
            log("GL error suppression : DISABLED (banner / 1283 spam will return)");
        }

        // [errors] chat bar filter.
        if (PreFinalConfig.enableChatFilter) {
            ChatSpamFilter filter = new ChatSpamFilter();
            MinecraftForge.EVENT_BUS.register(filter);
            FMLCommonHandler.instance().bus().register(filter);
            if (PreFinalConfig.selfTest) {
                filter.selfTest();
            }
            log("chat filter          : enabled");
        } else {
            log("chat filter          : DISABLED");
        }

        // [debug] KHR_debug probe, off by default.
        if (PreFinalConfig.enableGlDebugProbe) {
            GLDebugProbe probe = new GLDebugProbe();
            FMLCommonHandler.instance().bus().register(probe);
            MinecraftForge.EVENT_BUS.register(probe);
            log("GL debug probe       : enabled");
        } else {
            log("GL debug probe       : disabled");
        }
    }
}
