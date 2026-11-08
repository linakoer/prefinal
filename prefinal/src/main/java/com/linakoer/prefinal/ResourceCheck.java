package com.linakoer.prefinal;

import java.io.InputStream;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;

/**
 * One-shot diagnostic: asks the live IResourceManager whether the textures the resource pack in
 * resourcepacks/forge1710-texturefix is supposed to provide are actually visible, and which packs
 * the game currently has selected. Writes to gldebug-probe.log.
 */
public final class ResourceCheck {

    private static final String[] PATHS = {
        "textures/items/focusHeal.png",
        "textures/items/item.steelsword.png",
        "textures/items/itemAspect.png",
        "textures/blocks/planks.png",
        "textures/blocks/gaiaHeadBlock_skeleton.png",
        "textures/items/apple.png",          // vanilla control, must always exist
        "textures/items/doesNotExist.png"    // negative control
    };

    private static boolean done;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (done || event.phase != Phase.START) {
            return;
        }
        done = true;
        try {
            StringBuilder packs = new StringBuilder();
            ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();
            List<?> entries = repo.getRepositoryEntries();
            for (int i = 0; i < entries.size(); i++) {
                if (packs.length() > 0) {
                    packs.append(", ");
                }
                packs.append(String.valueOf(entries.get(i)));
            }
            GLDebugProbe.writeExternal("[RESCHECK] selected packs: " + packs);

            IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
            for (String p : PATHS) {
                String result;
                try {
                    InputStream in = rm.getResource(new ResourceLocation("minecraft", p)).getInputStream();
                    in.close();
                    result = "FOUND";
                } catch (Throwable t) {
                    result = "MISSING (" + t.getClass().getSimpleName() + ")";
                }
                GLDebugProbe.writeExternal("[RESCHECK] minecraft:" + p + " -> " + result);
            }
        } catch (Throwable t) {
            GLDebugProbe.writeExternal("[RESCHECK] failed: " + t);
        }
    }
}
