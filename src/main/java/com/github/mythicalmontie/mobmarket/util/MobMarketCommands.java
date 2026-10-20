package com.github.mythicalmontie.mobmarket.util;

import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import com.github.mythicalmontie.mobmarket.config.CurrencyManager;
import com.github.mythicalmontie.mobmarket.config.MobAttributeModifier;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class MobMarketCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("mobmarket")
                        .then(Commands.literal("manifest")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");

                                            BlockEntity be = player.level().getBlockEntity(pos);
                                            if (be instanceof MobMarketBlockEntity market) {
                                                market.sendItemizedBreakdownToPlayer(player);
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("inspect")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    Entity player = source.getEntity();
                                    if (player == null) {
                                        source.sendFailure(Component.translatable("commands.mobmarket.inspect.not_player"));
                                        return 0;
                                    }

                                    Entity targetEntity = getTargetEntity(player, 5.0D);
                                    if (!(targetEntity instanceof LivingEntity living)) {
                                        source.sendFailure(Component.translatable("commands.mobmarket.inspect.no_target"));
                                        return 0;
                                    }

                                    source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.header", living.getType().getDescription()));
                                    List<MobAttributeModifier> modifiers = null;
                                    try {
                                        Field attributesField = CurrencyManager.class.getDeclaredField("MOB_ATTRIBUTES");
                                        attributesField.setAccessible(true);
                                        @SuppressWarnings("unchecked")
                                        var mobAttributesMap = (java.util.Map<net.minecraft.world.entity.EntityType<?>, List<MobAttributeModifier>>) attributesField.get(null);

                                        modifiers = mobAttributesMap.get(living.getType());
                                    } catch (Exception ignored) {}

                                    CompoundTag nbt = new CompoundTag();
                                    living.save(nbt);

                                    source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.keys_title"));
                                    MutableComponent keysComponent = Component.literal(" ");
                                    var allKeys = new ArrayList<>(nbt.getAllKeys());

                                    for (int i = 0; i < allKeys.size(); i++) {
                                        String key = allKeys.get(i);

                                        boolean isConfigured = false;
                                        if (modifiers != null) {
                                            for (MobAttributeModifier mod : modifiers) {
                                                if (mod.getKey().equals(key)) {
                                                    isConfigured = true;
                                                    break;
                                                }
                                            }
                                        }

                                        String colorCode = isConfigured ? "§a" : "§7";
                                        keysComponent.append(Component.literal(colorCode + key));
                                        if (i < allKeys.size() - 1) {
                                            keysComponent.append(Component.literal("§8, "));
                                        }
                                    }
                                    source.sendSystemMessage(keysComponent);

                                    int basePrice = CurrencyManager.getBasePriceForMob(living.getType());
                                    String currencyId = CurrencyManager.getCurrencyIdForMob(living.getType());
                                    source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.base_price", basePrice, CurrencyManager.getCurrencyName(currencyId, basePrice)));

                                    source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.modifiers_title"));
                                    if (modifiers != null && !modifiers.isEmpty()) {
                                        for (MobAttributeModifier mod : modifiers) {
                                            source.sendSystemMessage(Component.translatable(
                                                    "commands.mobmarket.inspect.modifier_item",
                                                    mod.getKey(), mod.getType(), mod.getThreshold(), mod.getBaseline(), mod.getMultiplierPerUnit(), mod.getBonusAmount()
                                            ));
                                        }
                                    } else {
                                        source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.no_modifiers"));
                                    }

                                    int finalPrice = CurrencyManager.calculateFinalPrice(living);
                                    source.sendSystemMessage(Component.translatable("commands.mobmarket.inspect.price", finalPrice, CurrencyManager.getCurrencyName(currencyId, finalPrice)));

                                    return 1;
                                })
                        )
        );
    }

    private static Entity getTargetEntity(Entity player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        Entity targetedEntity = null;
        double closestDistanceSq = maxDistance * maxDistance;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(maxDistance))) {
            if (entity.isSpectator() || !entity.isPickable()) continue;

            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            Vec3 toEntity = entityPos.subtract(eyePos);

            double distanceSq = toEntity.lengthSqr();
            if (distanceSq > closestDistanceSq) continue;

            Vec3 normalizedToEntity = toEntity.normalize();
            double dot = lookVec.dot(normalizedToEntity);

            if (dot > 0.90) {
                closestDistanceSq = distanceSq;
                targetedEntity = entity;
            }
        }
        return targetedEntity;
    }
}