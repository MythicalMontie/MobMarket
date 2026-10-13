package com.github.mythicalmontie.mobmarket.network;

import com.github.mythicalmontie.mobmarket.MobMarket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {

        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MobMarket.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE.messageBuilder(SyncCurrencies.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCurrencies::encode)
                .decoder(SyncCurrencies::new)
                .consumerMainThread(SyncCurrencies::handle)
                .add();

    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllPlayers(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }


    public static void sendToServer(Object message) {
        if (INSTANCE != null) {
            INSTANCE.sendToServer(message);
        }
    }
}