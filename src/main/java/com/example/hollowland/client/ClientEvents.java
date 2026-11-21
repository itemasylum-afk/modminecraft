package com.example.hollowland.client;

import com.example.hollowland.HollowlandMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HollowlandMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Watcher: tall (1.35x) and pale.  Hollow Child: small (0.6x) and dark.
        event.registerEntityRenderer(HollowlandMod.WATCHER.get(),
                ctx -> new HollowRenderer<>(ctx, "watcher", 1.35F));
        event.registerEntityRenderer(HollowlandMod.HOLLOW_CHILD.get(),
                ctx -> new HollowRenderer<>(ctx, "hollow_child", 0.6F));
    }
}
