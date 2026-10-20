package com.github.mythicalmontie.mobmarket.network;

import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaleBreakdown {
    private final BlockPos pos;

    public SaleBreakdown(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public static void handle(SaleBreakdown packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level().getBlockEntity(packet.pos) instanceof MobMarketBlockEntity marketBE) {
                if (marketBE.isPublic() || (marketBE.getOwnerUUID() != null && marketBE.getOwnerUUID().equals(player.getUUID()))) {
                    marketBE.sendItemizedBreakdownToPlayer(player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
