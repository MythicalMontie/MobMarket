package com.github.mythicalmontie.mobmarket.blockentity;

import com.github.mythicalmontie.mobmarket.MobMarket;
import com.github.mythicalmontie.mobmarket.config.Config;
import com.github.mythicalmontie.mobmarket.config.CurrencyManager;
import com.github.mythicalmontie.mobmarket.util.TeamProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MobMarketBlockEntity extends BlockEntity implements MenuProvider, Nameable {

    public MobMarketBlockEntity(BlockPos pos, BlockState state) {
        super(MobMarket.MOB_MARKET_BE.get(), pos, state);
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    public ItemStackHandler getInventory() {
        return this.itemHandler;
    }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override public Component getName() {
        return Component.translatable("container.mobmarket.mob_market");
    }
    @Override public Component getDisplayName() {
        return getName();
    }

    private UUID ownerUUID;
    private String ownerName = "Unknown";
    private int accessMode = 0;
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }
    public String getOwnerName() {
        return this.ownerName;
    }
    public void setOwner(Player player) {
        if (player != null) {
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();
            setChanged();
            syncDataToClient();
        }
    }
    public void setAccessMode(int mode) {
        this.accessMode = mode % 3;
        setChanged();
        syncDataToClient();
    }
    public boolean isPrivate() {
        return this.accessMode == 0;
    }
    public boolean isTeam() {
        return this.accessMode == 1;
    }
    public boolean isPublic() {
        return this.accessMode == 2;
    }
    public void setPublic(boolean isPublic) {
        setAccessMode(isPublic ? 2 : 0);
    }
    public void togglePublicState() {
        if (TeamProtectionHelper.isFTBTeamsLoaded()) {
            setAccessMode((this.accessMode + 1) % 3);
        } else {
            setAccessMode(this.accessMode == 2 ? 0 : 2);
        }
    }
    public void togglePublicStatus() {
        this.togglePublicState();
    }
    private void syncDataToClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private BlockPos corner1;
    private BlockPos corner2;
    public void setCustomBounds(BlockPos c1, BlockPos c2) {
        this.corner1 = c1;
        this.corner2 = c2;
        triggerBoundaryFlash();
    }
    private int boundaryFlashTicks = 0;
    public boolean shouldFlashBoundary() {
        return this.boundaryFlashTicks > 0;
    }
    public void tickClient() {
        if (this.boundaryFlashTicks > 0) {
            this.boundaryFlashTicks--;
        }
    }
    public void triggerBoundaryFlash() {
        this.boundaryFlashTicks = 60;
        setChanged();
        syncDataToClient();
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        if (this.boundaryFlashTicks > 0) {
            tag.putInt("BoundaryFlashTicks", this.boundaryFlashTicks);
        }
        return tag;
    }

    public AABB getScanningArea() {
        if (this.corner1 != null && this.corner2 != null) {
            return new AABB(this.corner1, this.corner2).expandTowards(1.0, 1.0, 1.0);
        }
        int radius = 5;
        return new AABB(this.worldPosition).inflate(radius);
    }

    public static String formatMobName(net.minecraft.world.entity.EntityType<?> entityType, int count) {
        String descriptionId = entityType.getDescriptionId();
        String baseName = net.minecraft.network.chat.Component.translatable(descriptionId).getString();

        if (count == 1) {
            return baseName;
        }

        net.minecraft.resources.ResourceLocation registryKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (registryKey == null) {
            return baseName + "s";
        }
        String registryId = registryKey.toString();
        String namespace = registryKey.getNamespace();
        boolean isInvariant = false;
        if (Config.invariantPlurals != null) {
            for (String entry : Config.invariantPlurals) {
                if (entry.equalsIgnoreCase(registryId) || entry.equalsIgnoreCase(namespace)) {isInvariant = true; break;}
            }
        }
        if (isInvariant) {
            return baseName;
        }
        String lowerName = baseName.toLowerCase();
        if (lowerName.endsWith("man")) {
            return baseName.substring(0, baseName.length() - 3) + (baseName.endsWith("Man") ? "Men" : "men");
        }
        return baseName + "s";
    }

    private final List<SaleRecord> lastSalesRecord = new ArrayList<>();
    private int lastEligibleRemaining = 0;
    private int lastIneligibleCount = 0;

    private int liveEligibleCount = 0;
    private int liveIneligibleCount = 0;
    private int liveBabyIneligibleCount = 0;
    private int liveUnconfiguredIneligibleCount = 0;
    private final Map<String, Integer> liveCurrencyTotals = new LinkedHashMap<>();
    private final Map<String, ItemStack> liveCurrencyIcons = new LinkedHashMap<>();

    public void refreshLiveCounts() {
        if (this.level == null || this.level.isClientSide) return;

        AABB scanningArea = getScanningArea();
        List<Mob> entities = this.level.getEntitiesOfClass(Mob.class, scanningArea);

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
        setChanged();
        syncDataToClient();
    }
    public static class SaleRecord {
        private final net.minecraft.world.entity.EntityType<?> entityType;
        private final String mobTranslationKey;
        private final String currencyId;
        private int count;
        private int totalPrice;

        public SaleRecord(net.minecraft.world.entity.EntityType<?> entityType, String mobTranslationKey, String currencyId, int count, int totalPrice) {
            this.entityType = entityType;
            this.mobTranslationKey = mobTranslationKey;
            this.currencyId = currencyId;
            this.count = count;
            this.totalPrice = totalPrice;
        }

        public void add(int price) {
            this.count++;
            this.totalPrice += price;
        }

        public int getCount() { return count; }
        public int getTotalPrice() { return totalPrice; }
        public String getMobTranslationKey() { return mobTranslationKey; }
        public String getCurrencyId() { return currencyId; }
        public net.minecraft.world.entity.EntityType<?> getEntityType() { return entityType; }
    }
    private void payoutCurrencies(Map<String, Integer> totals) {
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            String currencyId = entry.getKey();
            int amount = entry.getValue();

            Map<Item, Integer> worthMap = CurrencyManager.getCurrencyValuesMap(currencyId);
            if (worthMap == null || worthMap.isEmpty()) continue;

            List<Map.Entry<Item, Integer>> sortedItems = new ArrayList<>(worthMap.entrySet());
            sortedItems.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            for (Map.Entry<Item, Integer> itemEntry : sortedItems) {
                Item item = itemEntry.getKey();
                int itemValue = itemEntry.getValue();

                int count = amount / itemValue;
                if (count > 0) {
                    amount %= itemValue;
                    ItemStack payoutStack = new ItemStack(item, count);

                    for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
                        payoutStack = this.itemHandler.insertItem(slot, payoutStack, false);
                        if (payoutStack.isEmpty()) break;
                    }
                }
            }
        }
    }

    public void processMarketScan() {
        if (this.level == null || this.level.isClientSide) return;

        AABB scanningArea = getScanningArea();
        List<Mob> entities = this.level.getEntitiesOfClass(Mob.class, scanningArea);
        Map<String, Integer> accumulatedCurrency = new HashMap<>();

        this.lastSalesRecord.clear();
        this.lastEligibleRemaining = 0;
        this.lastIneligibleCount = 0;
        int totalSold = 0;

        for (Mob mob : entities) {
            if (!mob.isAlive()) continue;
            var type = mob.getType();

            if (CurrencyManager.isMobConfigured(type)) {
                if (mob instanceof Animal animal && animal.isBaby() && !CurrencyManager.areBabiesEligible(type)) {
                    this.lastIneligibleCount++;
                    continue;
                }

                int chance = Config.saleChance;
                if (chance < 100 && this.level.random.nextInt(100) >= chance) {
                    this.lastEligibleRemaining++;
                    continue;
                }

                int finalPrice = CurrencyManager.calculateFinalPrice(mob);
                String currencyId = CurrencyManager.getCurrencyIdForMob(type);

                if (currencyId != null && finalPrice > 0) {
                    accumulatedCurrency.put(currencyId, accumulatedCurrency.getOrDefault(currencyId, 0) + finalPrice);

                    String mobNameKey = type.getDescriptionId();
                    SaleRecord existingRecord = null;
                    for (SaleRecord record : this.lastSalesRecord) {
                        if (record.getMobTranslationKey().equals(mobNameKey) && record.getCurrencyId().equals(currencyId)) {
                            existingRecord = record;
                            break;
                        }
                    }

                    if (existingRecord != null) {
                        existingRecord.add(finalPrice);
                    } else {
                        this.lastSalesRecord.add(new SaleRecord(type, mobNameKey, currencyId, 1, finalPrice));
                    }

                    mob.discard();
                    totalSold++;
                } else {
                    this.lastIneligibleCount++;
                }
            } else {
                this.lastIneligibleCount++;
            }
        }

        if (totalSold > 0 || this.lastEligibleRemaining > 0 || this.lastIneligibleCount > 0) {
            if (totalSold > 0) {
                payoutCurrencies(accumulatedCurrency);
            }

            if (Config.sendSaleMessages && this.level.getServer() != null) {
                String cleanCommand = "/mobmarket manifest " + this.worldPosition.getX() + " " + this.worldPosition.getY() + " " + this.worldPosition.getZ();

                for (ServerPlayer onlinePlayer : this.level.getServer().getPlayerList().getPlayers()) {
                    boolean shouldNotify = false;

                    if (this.accessMode == 2) {
                        shouldNotify = true;
                    } else if (this.accessMode == 1) {
                        shouldNotify = TeamProtectionHelper.canAccess(onlinePlayer, this.ownerUUID, true);
                    } else if (this.accessMode == 0) {
                        shouldNotify = this.ownerUUID != null && onlinePlayer.getUUID().equals(this.ownerUUID);
                    }

                    if (shouldNotify) {
                        if (totalSold > 0) {
                            String mobTermKey = (totalSold == 1) ? "text.mobmarket.mob_singular" : "text.mobmarket.mob_plural";
                            Component mobTermComponent = Component.translatable(mobTermKey);
                            String translationKey = (totalSold == 1) ? "message.mobmarket.sale_singular" : "message.mobmarket.sale_plural";

                            String currencyId = accumulatedCurrency.keySet().iterator().next();
                            int totalPrice = accumulatedCurrency.get(currencyId);
                            String currencyName = CurrencyManager.getCurrencyName(currencyId, totalPrice);

                            Component saleMessage = Component.translatable(
                                    translationKey,
                                    totalSold,
                                    mobTermComponent,
                                    totalPrice,
                                    currencyName
                            ).withStyle(net.minecraft.ChatFormatting.GREEN);

                            Component buttonPrompt = Component.translatable(
                                    (totalSold == 1) ? "text.mobmarket.invoice_button_singular" : "text.mobmarket.invoice_button_plural",
                                    mobTermComponent
                            ).withStyle(style -> style
                                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                            net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                            cleanCommand
                                    ))
                                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable("text.mobmarket.hover_breakdown")
                                    ))
                            );

                            onlinePlayer.sendSystemMessage(saleMessage);
                            onlinePlayer.sendSystemMessage(buttonPrompt);
                        } else if (this.lastEligibleRemaining > 0) {
                            String mobTermKey = (this.lastEligibleRemaining == 1) ? "text.mobmarket.mob_singular" : "text.mobmarket.mob_plural";
                            String remainKey = (this.lastEligibleRemaining == 1) ? "text.mobmarket.invoice_remaining_singular" : "text.mobmarket.invoice_remaining_plural";

                            Component remainMessage;
                            if (this.lastEligibleRemaining == 1) {
                                remainMessage = Component.translatable(
                                        remainKey,
                                        "1 " + Component.translatable(mobTermKey).getString()
                                );
                            } else {
                                remainMessage = Component.translatable(
                                        remainKey,
                                        this.lastEligibleRemaining,
                                        Component.translatable(mobTermKey)
                                );
                            }

                            onlinePlayer.sendSystemMessage(remainMessage.copy().withStyle(style -> style
                                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                            net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                            cleanCommand
                                    ))
                                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable("text.mobmarket.hover_breakdown")
                                    ))
                            ));
                        }
                    }
                }
            }
        }
    }

    public void sendItemizedBreakdownToPlayer(ServerPlayer player) {
        if (player == null) return;

        player.sendSystemMessage(Component.translatable("message.mobmarket.invoice_header")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));

        if (this.lastSalesRecord.isEmpty() && this.lastEligibleRemaining == 0 && this.lastIneligibleCount == 0) {
            player.sendSystemMessage(Component.translatable("message.mobmarket.no_logs")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            for (SaleRecord record : this.lastSalesRecord) {
                String lineKey = (record.getCount() == 1) ? "message.mobmarket.invoice_line_singular" : "message.mobmarket.invoice_line_plural";
                String smartMobName = formatMobName(record.getEntityType(), record.getCount());
                String countAndName = record.getCount() + " " + smartMobName;
                String currencyName = CurrencyManager.getCurrencyName(record.getCurrencyId(), record.getTotalPrice());

                player.sendSystemMessage(Component.translatable(
                        lineKey,
                        countAndName,
                        record.getTotalPrice(),
                        currencyName
                ).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }

            if (this.lastEligibleRemaining > 0) {
                String remainKey = (this.lastEligibleRemaining == 1) ? "text.mobmarket.invoice_remaining_singular" : "text.mobmarket.invoice_remaining_plural";
                String mobTermKey = (this.lastEligibleRemaining == 1) ? "text.mobmarket.mob_singular" : "text.mobmarket.mob_plural";
                Component mobTermComponent = Component.translatable(mobTermKey);

                if (this.lastEligibleRemaining == 1) {
                    player.sendSystemMessage(Component.translatable(
                            remainKey,
                            "1 " + mobTermComponent.getString()
                    ).withStyle(net.minecraft.ChatFormatting.GRAY));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            remainKey,
                            this.lastEligibleRemaining,
                            mobTermComponent
                    ).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }

            if (this.lastIneligibleCount > 0) {
                String ineligKey = (this.lastIneligibleCount == 1) ? "text.mobmarket.invoice_ineligible_singular" : "text.mobmarket.invoice_ineligible_plural";
                String mobTermKey = (this.lastIneligibleCount == 1) ? "text.mobmarket.mob_singular" : "text.mobmarket.mob_plural";
                Component mobTermComponent = Component.translatable(mobTermKey);

                if (this.lastIneligibleCount == 1) {
                    player.sendSystemMessage(Component.translatable(
                            ineligKey,
                            "1 " + mobTermComponent.getString()
                    ).withStyle(net.minecraft.ChatFormatting.RED));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            ineligKey,
                            this.lastIneligibleCount,
                            mobTermComponent
                    ).withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.mobmarket.invoice_footer")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MobMarketBlockEntity blockEntity) {
        blockEntity.tickClient();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, MobMarketBlockEntity blockEntity) {
        if (level.getDayTime() % 24000L == (long) Config.targetCheckTick) {
            blockEntity.processMarketScan();
        }
    }

    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.itemHandler.serializeNBT());
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
            tag.putString("OwnerName", this.ownerName);
        }
        tag.putInt("AccessMode", this.accessMode);

        if (this.corner1 != null) {
            tag.putLong("Corner1", this.corner1.asLong());
        }
        if (this.corner2 != null) {
            tag.putLong("Corner2", this.corner2.asLong());
        }

        tag.putInt("LiveEligible", this.liveEligibleCount);
        tag.putInt("LiveIneligible", this.liveIneligibleCount);
        tag.putInt("LiveBabyIneligible", this.liveBabyIneligibleCount);
        tag.putInt("LiveUnconfiguredIneligible", this.liveUnconfiguredIneligibleCount);

        CompoundTag currencyTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.liveCurrencyTotals.entrySet()) {
            currencyTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("LiveCurrencyTotals", currencyTag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            this.itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("OwnerName")) {
            this.ownerName = tag.getString("OwnerName");
        }

        if (tag.contains("AccessMode")) {
            this.accessMode = tag.getInt("AccessMode");
        } else if (tag.contains("IsPublic")) {
            this.accessMode = tag.getBoolean("IsPublic") ? 2 : 0;
        }

        if (tag.contains("Corner1")) {
            this.corner1 = BlockPos.of(tag.getLong("Corner1"));
        }
        if (tag.contains("Corner2")) {
            this.corner2 = BlockPos.of(tag.getLong("Corner2"));
        }

        if (tag.contains("BoundaryFlashTicks")) {
            this.boundaryFlashTicks = tag.getInt("BoundaryFlashTicks");
        }

        this.liveEligibleCount = tag.getInt("LiveEligible");
        this.liveIneligibleCount = tag.getInt("LiveIneligible");
        this.liveBabyIneligibleCount = tag.getInt("LiveBabyIneligible");
        this.liveUnconfiguredIneligibleCount = tag.getInt("LiveUnconfiguredIneligible");

        this.liveCurrencyTotals.clear();
        if (tag.contains("LiveCurrencyTotals", 10)) {
            CompoundTag currencyTag = tag.getCompound("LiveCurrencyTotals");
            for (String key : currencyTag.getAllKeys()) {
                int total = currencyTag.getInt(key);
                this.liveCurrencyTotals.put(key, total);

                if (!this.liveCurrencyIcons.containsKey(key)) {
                    Map<Item, Integer> worthMap = CurrencyManager.getCurrencyValuesMap(key);
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
                            this.liveCurrencyIcons.put(key, new ItemStack(lowestValueItem));
                        }
                    }
                }
            }
        }
    }

    @Nullable
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        if (this.level != null && !this.level.isClientSide) {
            refreshLiveCounts();
        }
        return new MobMarketGUI(windowId, playerInventory, this.itemHandler, this.worldPosition, this.level);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
