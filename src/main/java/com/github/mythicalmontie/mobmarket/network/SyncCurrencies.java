package com.github.mythicalmontie.mobmarket.network;

import com.github.mythicalmontie.mobmarket.config.CurrencyManager;
import com.github.mythicalmontie.mobmarket.config.MobAttributeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCurrencies {
    private final Map<String, Map<Item, Integer>> currencyValues;
    private final Map<String, String> singularNames;
    private final Map<String, String> pluralNames;
    private final Map<EntityType<?>, String> mobToCurrency;
    private final Map<EntityType<?>, Integer> mobBasePrices;
    private final Map<EntityType<?>, Boolean> mobAllowBabies;
    private final Map<EntityType<?>, List<MobAttributeModifier>> mobAttributes;

    public SyncCurrencies(Map<String, Map<Item, Integer>> currencyValues,
                          Map<String, String> singularNames,
                          Map<String, String> pluralNames,
                          Map<EntityType<?>, String> mobToCurrency,
                          Map<EntityType<?>, Integer> mobBasePrices,
                          Map<EntityType<?>, Boolean> mobAllowBabies,
                          Map<EntityType<?>, List<MobAttributeModifier>> mobAttributes) {
        this.currencyValues = currencyValues;
        this.singularNames = singularNames;
        this.pluralNames = pluralNames;
        this.mobToCurrency = mobToCurrency;
        this.mobBasePrices = mobBasePrices;
        this.mobAllowBabies = mobAllowBabies;
        this.mobAttributes = mobAttributes;
    }

    public SyncCurrencies(FriendlyByteBuf buf) {
        this.currencyValues = new HashMap<>();
        this.singularNames = new HashMap<>();
        this.pluralNames = new HashMap<>();
        this.mobToCurrency = new HashMap<>();
        this.mobBasePrices = new HashMap<>();
        this.mobAllowBabies = new HashMap<>();
        this.mobAttributes = new HashMap<>();

        int currencyGroupCount = buf.readVarInt();
        for (int i = 0; i < currencyGroupCount; i++) {
            String currencyId = buf.readUtf();
            String singular = buf.readUtf();
            String plural = buf.readUtf();

            this.singularNames.put(currencyId, singular);
            this.pluralNames.put(currencyId, plural);

            int itemCount = buf.readVarInt();
            Map<Item, Integer> itemValues = new HashMap<>();
            for (int j = 0; j < itemCount; j++) {
                Item item = buf.readById(BuiltInRegistries.ITEM);
                int value = buf.readVarInt();
                if (item != null) itemValues.put(item, value);
            }
            this.currencyValues.put(currencyId, itemValues);
        }

        int mobToCurrencyCount = buf.readVarInt();
        for (int i = 0; i < mobToCurrencyCount; i++) {
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            String currencyId = buf.readUtf();
            if (type != null) this.mobToCurrency.put(type, currencyId);
        }

        int mobBasePriceCount = buf.readVarInt();
        for (int i = 0; i < mobBasePriceCount; i++) {
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            int basePrice = buf.readVarInt();
            if (type != null) this.mobBasePrices.put(type, basePrice);
        }

        int mobAllowBabiesCount = buf.readVarInt();
        for (int i = 0; i < mobAllowBabiesCount; i++) {
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            boolean allowBabies = buf.readBoolean();
            if (type != null) this.mobAllowBabies.put(type, allowBabies);
        }

        int mobAttrCount = buf.readVarInt();
        for (int i = 0; i < mobAttrCount; i++) {
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            int modifierCount = buf.readVarInt();
            List<MobAttributeModifier> modifiers = new ArrayList<>();
            for (int j = 0; j < modifierCount; j++) {
                String key = buf.readUtf();
                String modifierType = buf.readUtf();
                double baseline = buf.readDouble();
                double multiplierPerUnit = buf.readDouble();
                double threshold = buf.readDouble();
                int bonusAmount = buf.readVarInt();
                double minMultiplier = buf.readDouble();
                double maxMultiplier = buf.readDouble();

                modifiers.add(new MobAttributeModifier(
                        key, modifierType, baseline, multiplierPerUnit, threshold, bonusAmount, minMultiplier, maxMultiplier
                ));
            }
            if (type != null) this.mobAttributes.put(type, modifiers);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.currencyValues.size());
        for (Map.Entry<String, Map<Item, Integer>> entry : this.currencyValues.entrySet()) {
            String currencyId = entry.getKey();
            buf.writeUtf(currencyId);
            buf.writeUtf(this.singularNames.getOrDefault(currencyId, "Unit"));
            buf.writeUtf(this.pluralNames.getOrDefault(currencyId, "Units"));

            buf.writeVarInt(entry.getValue().size());
            for (Map.Entry<Item, Integer> itemEntry : entry.getValue().entrySet()) {
                buf.writeId(BuiltInRegistries.ITEM, itemEntry.getKey());
                buf.writeVarInt(itemEntry.getValue());
            }
        }

        buf.writeVarInt(this.mobToCurrency.size());
        for (Map.Entry<EntityType<?>, String> entry : this.mobToCurrency.entrySet()) {
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, entry.getKey());
            buf.writeUtf(entry.getValue());
        }

        buf.writeVarInt(this.mobBasePrices.size());
        for (Map.Entry<EntityType<?>, Integer> entry : this.mobBasePrices.entrySet()) {
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        buf.writeVarInt(this.mobAllowBabies.size());
        for (Map.Entry<EntityType<?>, Boolean> entry : this.mobAllowBabies.entrySet()) {
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        buf.writeVarInt(this.mobAttributes.size());
        for (Map.Entry<EntityType<?>, List<MobAttributeModifier>> entry : this.mobAttributes.entrySet()) {
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (MobAttributeModifier mod : entry.getValue()) {
                buf.writeUtf(mod.getKey());
                buf.writeUtf(mod.getType());
                buf.writeDouble(mod.getBaseline());
                buf.writeDouble(mod.getMultiplierPerUnit());
                buf.writeDouble(mod.getThreshold());
                buf.writeVarInt(mod.getBonusAmount());
                buf.writeDouble(mod.getMinMultiplier());
                buf.writeDouble(mod.getMaxMultiplier());
            }
        }
    }

    public static void handle(SyncCurrencies packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            CurrencyManager.setClientCache(
                    packet.currencyValues,
                    packet.singularNames,
                    packet.pluralNames,
                    packet.mobToCurrency,
                    packet.mobBasePrices,
                    packet.mobAllowBabies,
                    packet.mobAttributes
            );
        });
        context.setPacketHandled(true);
    }
}