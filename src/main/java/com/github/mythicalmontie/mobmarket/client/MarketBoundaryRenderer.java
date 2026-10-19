package com.github.mythicalmontie.mobmarket.client;

import com.github.mythicalmontie.mobmarket.MobMarket;
import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MobMarket.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MarketBoundaryRenderer {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Level level = mc.level;
        double maxRenderDistanceSq = 1024.0;

        if (BoundarySelectionHandler.isSelectionModeActive()) {
            BlockPos targetPos = BoundarySelectionHandler.getTargetMarketPos();
            if (targetPos == null) return;

            if (mc.player.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()) <= maxRenderDistanceSq) {
                if (level.hasChunksAt(targetPos, targetPos)) {
                    BlockEntity be = level.getBlockEntity(targetPos);
                    if (be instanceof MobMarketBlockEntity marketBE) {
                        renderBoxParticles(level, marketBE.getScanningArea());
                    }
                }

                BlockPos c1 = BoundarySelectionHandler.getCornerOne();
                if (c1 != null) {
                    renderCornerPillar(level, c1);
                }
            }
        }
        else {
            BlockPos playerPos = mc.player.blockPosition();
            BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

            for (int x = -16; x <= 16; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -16; z <= 16; z++) {
                        searchPos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);

                        if (level.hasChunksAt(searchPos, searchPos)) {
                            BlockEntity be = level.getBlockEntity(searchPos);
                            if (be instanceof MobMarketBlockEntity marketBE && marketBE.shouldFlashBoundary()) {
                                renderBoxParticles(level, marketBE.getScanningArea());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderCornerPillar(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double baseY = pos.getY();

        for (double yOffset = 0; yOffset <= 4.0; yOffset += 0.25) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, centerX, baseY + yOffset, centerZ, 0, 0, 0);
        }
    }

    private static void renderBoxParticles(Level level, AABB box) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;

        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        for (double x = minX; x <= maxX; x += 0.5) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, minY, minZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, maxY, minZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, minY, maxZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, maxY, maxZ, 0, 0, 0);
        }

        for (double z = minZ; z <= maxZ; z += 0.5) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, minY, z, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, minY, z, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, maxY, z, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, maxY, z, 0, 0, 0);
        }

        for (double y = minY; y <= maxY; y += 0.5) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, y, minZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, y, minZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, y, maxZ, 0, 0, 0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, y, maxZ, 0, 0, 0);
        }
    }
}