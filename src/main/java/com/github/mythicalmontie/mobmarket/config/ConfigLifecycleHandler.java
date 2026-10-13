package com.github.mythicalmontie.mobmarket.config;

import com.github.mythicalmontie.mobmarket.MobMarket;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MobMarket.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigLifecycleHandler {

    private static final CurrencyManager.CurrencyLoader CURRENCY_LOADER = new CurrencyManager.CurrencyLoader();
    private static final CurrencyManager.MappingLoader MAPPING_LOADER = new CurrencyManager.MappingLoader();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CURRENCY_LOADER);
        event.addListener(MAPPING_LOADER);
        CurrencyManager.LOGGER.info("Mob Market JSON Datapack listeners successfully registered!");
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            com.github.mythicalmontie.mobmarket.network.ModMessages.sendToPlayer(
                    CurrencyManager.createSyncPacket(),
                    event.getPlayer()
            );
        } else {
            com.github.mythicalmontie.mobmarket.network.ModMessages.sendToAllPlayers(
                    CurrencyManager.createSyncPacket()
            );
        }
    }
}