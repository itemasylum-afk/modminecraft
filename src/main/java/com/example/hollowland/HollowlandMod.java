package com.example.hollowland;

import com.example.hollowland.entity.HollowChildEntity;
import com.example.hollowland.entity.WatcherEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Hollowland: two entities (Watcher, Hollow Child) and one dimension (hollowland:hollowland).
 */
@Mod(HollowlandMod.MOD_ID)
public class HollowlandMod {
    public static final String MOD_ID = "hollowland";

    /** How long a player may stay in the Hollowland before it lets them go (ticks). 6000 = 5 minutes. */
    public static final int STAY_TICKS = 6000;
    private static final String TIME_TAG = "hollowland_time";

    public static final ResourceKey<Level> HOLLOWLAND =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MOD_ID, "hollowland"));

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.6F).clientTrackingRange(10).build("watcher"));

    public static final RegistryObject<EntityType<HollowChildEntity>> HOLLOW_CHILD = ENTITIES.register("hollow_child",
            () -> EntityType.Builder.of(HollowChildEntity::new, MobCategory.MONSTER)
                    .sized(0.45F, 1.1F).clientTrackingRange(8).build("hollow_child"));

    public static final RegistryObject<Item> WATCHER_SPAWN_EGG = ITEMS.register("watcher_spawn_egg",
            () -> new ForgeSpawnEggItem(WATCHER, 0x2B2B2B, 0xDDDDDD, new Item.Properties()));
    public static final RegistryObject<Item> HOLLOW_CHILD_SPAWN_EGG = ITEMS.register("hollow_child_spawn_egg",
            () -> new ForgeSpawnEggItem(HOLLOW_CHILD, 0x280A0C, 0xE1DCD2, new Item.Properties()));

    public HollowlandMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::registerSpawnPlacements);
        bus.addListener(this::addToCreativeTab);
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------------- mod bus

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WATCHER.get(), WatcherEntity.createAttributes().build());
        event.put(HOLLOW_CHILD.get(), HollowChildEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(WATCHER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WatcherEntity::checkWatcherSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(HOLLOW_CHILD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                HollowChildEntity::checkHollowSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(WATCHER_SPAWN_EGG);
            event.accept(HOLLOW_CHILD_SPAWN_EGG);
        }
    }

    // ------------------------------------------------------------- forge bus

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != HOLLOWLAND) {
            return;
        }
        int time = player.getPersistentData().getInt(TIME_TAG) + 1;
        player.getPersistentData().putInt(TIME_TAG, time);
        if (time >= STAY_TICKS) {
            releaseFromHollowland(player);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("hollowland")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("enter").executes(ctx -> {
                    pullIntoHollowland(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(ctx -> {
                    releaseFromHollowland(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    // --------------------------------------------------------------- helpers

    /** Sends the player to the Hollowland dimension. */
    public static void pullIntoHollowland(ServerPlayer player) {
        ServerLevel dest = player.server.getLevel(HOLLOWLAND);
        if (dest == null || player.level().dimension() == HOLLOWLAND) {
            return;
        }
        player.getPersistentData().putInt(TIME_TAG, 0);
        // The flat generator's grey ground surface sits at y = 12.
        player.teleportTo(dest, 0.5D, 12.0D, 0.5D, player.getYRot(), player.getXRot());
    }

    /** Returns the player to the Overworld (their spawn point, or world spawn). */
    public static void releaseFromHollowland(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos target = player.getRespawnDimension() == Level.OVERWORLD && player.getRespawnPosition() != null
                ? player.getRespawnPosition()
                : overworld.getSharedSpawnPos();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
        player.getPersistentData().putInt(TIME_TAG, 0);
        player.removeEffect(MobEffects.DARKNESS);
        player.teleportTo(overworld, target.getX() + 0.5D, y, target.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }
}
