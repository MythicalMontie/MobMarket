package com.github.mythicalmontie.mobmarket.network;

import com.github.mythicalmontie.mobmarket.MobMarket;
import com.github.mythicalmontie.mobmarket.config.CurrencyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MobMarket.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventSubscriber {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CurrencyManager.CurrencyLoader());
        event.addListener(new CurrencyManager.MappingLoader());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            com.github.mythicalmontie.mobmarket.network.ModMessages.sendToPlayer(
                    CurrencyManager.createSyncPacket(), serverPlayer
            );
        }
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            com.github.mythicalmontie.mobmarket.network.ModMessages.sendToPlayer(
                    CurrencyManager.createSyncPacket(), event.getPlayer()
            );
        } else {
            com.github.mythicalmontie.mobmarket.network.ModMessages.sendToAllPlayers(
                    CurrencyManager.createSyncPacket()
            );
        }
    }

}
