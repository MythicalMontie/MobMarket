package com.github.mythicalmontie.mobmarket.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.github.mythicalmontie.mobmarket.MobMarket.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BoundarySelectionHandler {
    private static boolean active = false;
    private static BlockPos targetBlockEntityPos = null;
    private static BlockPos cornerOne = null;

    public static void toggleSelectionMode(BlockPos marketPos) {
        Minecraft mc = Minecraft.getInstance();
        if (active && marketPos.equals(targetBlockEntityPos)) {
            active = false;
            targetBlockEntityPos = null;
            cornerOne = null;
            if (mc.player != null) {
                Component msg = Component.translatable("message.mobmarket.selection_canceled")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED));
                mc.player.displayClientMessage(msg, true);
            }
        } else {
            active = true;
            targetBlockEntityPos = marketPos;
            cornerOne = null;
            if (mc.player != null) {
                Component msg = Component.translatable("message.mobmarket.selection_started")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW));
                mc.player.displayClientMessage(msg, true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active || !event.isUseItem() || Minecraft.getInstance().player == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.player.getMainHandItem().isEmpty()) return;

        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos clickedPos = blockHit.getBlockPos();

            if (clickedPos.equals(targetBlockEntityPos) && mc.player.isShiftKeyDown()) return;

            event.setCanceled(true);

            if (cornerOne == null) {
                cornerOne = clickedPos;
                Component msg = Component.translatable("message.mobmarket.corner_one_set")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW));
                mc.player.displayClientMessage(msg, true);
            } else {
                active = false;
                Component msg = Component.translatable("message.mobmarket.selection_complete")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN));
                mc.player.displayClientMessage(msg, true);

                com.github.mythicalmontie.mobmarket.network.ModMessages.sendToServer(
                        new com.github.mythicalmontie.mobmarket.network.SaveBoundsPacket(targetBlockEntityPos, cornerOne, clickedPos)
                );

                targetBlockEntityPos = null;
                cornerOne = null;
            }
        }
    }

    public static boolean isSelectionModeActive() { return active; }
    public static BlockPos getTargetMarketPos() { return targetBlockEntityPos; }
    public static BlockPos getCornerOne() { return cornerOne; }
}