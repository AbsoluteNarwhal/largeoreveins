package com.absolutenarwhal.largeoreveins;

import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfigLoader;
import com.absolutenarwhal.largeoreveins.worldgen.OreVeinGenerator;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(LargeOreVeins.MOD_ID)
public class LargeOreVeins {
    public static final String MOD_ID = "largeoreveins";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final OreVeinConfigLoader ORE_VEIN_CONFIG_LOADER = new OreVeinConfigLoader();

    public LargeOreVeins(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ORE_VEIN_CONFIG_LOADER);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        OreVeinGenerator.onChunkLoad(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        OreVeinGenerator.onServerTick(event);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        OreVeinGenerator.clearGeneratedChunks();
    }
}
