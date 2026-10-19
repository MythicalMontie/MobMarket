package com.github.mythicalmontie.mobmarket.blockentity;

import com.github.mythicalmontie.mobmarket.MobMarket;
import com.github.mythicalmontie.mobmarket.config.CurrencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobMarketGUI extends AbstractContainerMenu {
    private final MobMarketBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    private int liveEligibleCount = 0;
    private int liveIneligibleCount = 0;
    private int liveBabyIneligibleCount = 0;
    private int liveUnconfiguredIneligibleCount = 0;

    private final Map<String, Integer> liveCurrencyTotals = new LinkedHashMap<>();
    private final Map<String, ItemStack> liveCurrencyIcons = new LinkedHashMap<>();

    public MobMarketGUI(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, new ItemStackHandler(9), extraData.readBlockPos(), playerInventory.player.level());
    }

    public MobMarketGUI(int windowId, Inventory playerInventory, ItemStackHandler handler, BlockPos pos, Level level) {
        super(MobMarket.MOB_MARKET_MENU.get(), windowId);
        this.levelAccess = ContainerLevelAccess.create(level, pos);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MobMarketBlockEntity marketBE) {
            this.blockEntity = marketBE;
        } else {
            throw new IllegalStateException("Clipped block entity registry token mismatch or missing instance parameters at node location!");
        }

        calculateLiveCounts();

        for (int col = 0; col < 9; col++) {
            this.addSlot(new SlotItemHandler(handler, col, 8 + col * 18, 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 76 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 134));
        }
    }

    private void calculateLiveCounts() {
        if (this.blockEntity == null || this.blockEntity.getLevel() == null) return;

        AABB scanningArea = this.blockEntity.getScanningArea();
        List<Mob> entities = this.blockEntity.getLevel().getEntitiesOfClass(Mob.class, scanningArea);

        this.liveEligibleCount = 0;
        this.liveIneligibleCount = 0;
        this.liveBabyIneligibleCount = 0;
        this.liveUnconfiguredIneligibleCount = 0;

        this.liveCurrencyTotals.clear();
        this.liveCurrencyIcons.clear();

        for (Mob mob : entities) {
            if (!mob.isAlive()) continue;
            var type = mob.getType();

            if (CurrencyManager.isMobConfigured(type)) {
                if (mob instanceof Animal animal && animal.isBaby() && !CurrencyManager.areBabiesEligible(type)) {
                    this.liveIneligibleCount++;
                    this.liveBabyIneligibleCount++;
                } else {
                    this.liveEligibleCount++;

                    int finalPrice = CurrencyManager.calculateFinalPrice(mob);
                    String targetCurrencyId = CurrencyManager.getCurrencyIdForMob(type);

                    if (targetCurrencyId != null && finalPrice > 0) {
                        this.liveCurrencyTotals.put(targetCurrencyId, this.liveCurrencyTotals.getOrDefault(targetCurrencyId, 0) + finalPrice);

                        if (!this.liveCurrencyIcons.containsKey(targetCurrencyId)) {
                            Map<Item, Integer> worthMap = CurrencyManager.getCurrencyValuesMap(targetCurrencyId);
                            if (worthMap != null && !worthMap.isEmpty()) {
                                Item lowestValueItem = null;
                                int lowestValueFound = Integer.MAX_VALUE;

                                for (Map.Entry<Item, Integer> entry : worthMap.entrySet()) {
                                    if (entry.getValue() < lowestValueFound) {
                                        lowestValueFound = entry.getValue();
                                        lowestValueItem = entry.getKey();
                                    }
                                }
                                if (lowestValueItem != null) {
                                    this.liveCurrencyIcons.put(targetCurrencyId, new ItemStack(lowestValueItem));
                                }
                            }
                        }
                    }
                }
            } else {
                this.liveIneligibleCount++;
                this.liveUnconfiguredIneligibleCount++;
            }
        }
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 9) {
                if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.levelAccess.evaluate((level, pos) ->
                player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
    }

    public MobMarketBlockEntity getBlockEntity() { return this.blockEntity; }
    public int getLiveEligibleCount() { return this.liveEligibleCount; }
    public int getLiveIneligibleCount() { return this.liveIneligibleCount; }
    public int getLiveBabyIneligibleCount() { return this.liveBabyIneligibleCount; }
    public int getLiveUnconfiguredIneligibleCount() { return this.liveUnconfiguredIneligibleCount; }

    public Map<String, Integer> getLiveCurrencyTotals() { return this.liveCurrencyTotals; }
    public Map<String, ItemStack> getLiveCurrencyIcons() { return this.liveCurrencyIcons; }
}
