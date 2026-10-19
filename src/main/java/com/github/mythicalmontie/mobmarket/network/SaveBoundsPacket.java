package com.github.mythicalmontie.mobmarket.network;

import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveBoundsPacket {
    private final BlockPos blockEntityPos;
    private final BlockPos corner1;
    private final BlockPos corner2;

    public SaveBoundsPacket(BlockPos blockEntityPos, BlockPos corner1, BlockPos corner2) {
        this.blockEntityPos = blockEntityPos;
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public static void encode(SaveBoundsPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.blockEntityPos);
        buf.writeBlockPos(packet.corner1);
        buf.writeBlockPos(packet.corner2);
    }

    public static SaveBoundsPacket decode(FriendlyByteBuf buf) {
        return new SaveBoundsPacket(buf.readBlockPos(), buf.readBlockPos(), buf.readBlockPos());
    }

    public static void handle(SaveBoundsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            double distanceSq = player.distanceToSqr(packet.blockEntityPos.getX(), packet.blockEntityPos.getY(), packet.blockEntityPos.getZ());
            if (distanceSq <= 256.0) {
                var level = player.serverLevel();

                if (level.hasChunkAt(packet.blockEntityPos)) {
                    BlockEntity be = level.getBlockEntity(packet.blockEntityPos);
                    if (be instanceof MobMarketBlockEntity marketBE) {

                        int sizeX = Math.abs(packet.corner1.getX() - packet.corner2.getX()) + 1;
                        int sizeY = Math.abs(packet.corner1.getY() - packet.corner2.getY()) + 1;
                        int sizeZ = Math.abs(packet.corner1.getZ() - packet.corner2.getZ()) + 1;

                        if (sizeX > 16 || sizeY > 16 || sizeZ > 16) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mobmarket.bounds_too_large")
                                    .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED)));
                            return;
                        }

                        marketBE.setCustomBounds(packet.corner1, packet.corner2);
                        marketBE.triggerBoundaryFlash();

                        level.playSound(null, packet.blockEntityPos,
                                SoundEvents.AMETHYST_BLOCK_BREAK,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}