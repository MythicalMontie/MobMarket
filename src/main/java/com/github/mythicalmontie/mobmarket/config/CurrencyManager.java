package com.github.mythicalmontie.mobmarket.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.*;

public class CurrencyManager {
    private static final Gson GSON = new Gson();
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, Map<Item, Integer>> CURRENCY_VALUES = new HashMap<>();
    private static final Map<String, String> CURRENCY_SINGULAR_NAMES = new HashMap<>();
    private static final Map<String, String> CURRENCY_PLURAL_NAMES = new HashMap<>();
    private static final Map<EntityType<?>, String> MOB_TO_CURRENCY = new HashMap<>();
    private static final Map<EntityType<?>, Integer> MOB_BASE_PRICES = new HashMap<>();
    private static final Map<EntityType<?>, Boolean> MOB_ALLOW_BABIES = new HashMap<>();
    private static final Map<EntityType<?>, List<MobAttributeModifier>> MOB_ATTRIBUTES = new HashMap<>();

    private static final Map<String, Map<Item, Integer>> CLIENT_CURRENCY_VALUES = new HashMap<>();
    private static final Map<String, String> CLIENT_CURRENCY_SINGULAR_NAMES = new HashMap<>();
    private static final Map<String, String> CLIENT_CURRENCY_PLURAL_NAMES = new HashMap<>();
    private static final Map<EntityType<?>, String> CLIENT_MOB_TO_CURRENCY = new HashMap<>();
    private static final Map<EntityType<?>, Integer> CLIENT_MOB_BASE_PRICES = new HashMap<>();
    private static final Map<EntityType<?>, Boolean> CLIENT_MOB_ALLOW_BABIES = new HashMap<>();
    private static final Map<EntityType<?>, List<MobAttributeModifier>> CLIENT_MOB_ATTRIBUTES = new HashMap<>();

    public static class CurrencyLoader extends SimpleJsonResourceReloadListener {
        public CurrencyLoader() { super(GSON, "currencies"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objectMap, ResourceManager mgr, ProfilerFiller profiler) {
            CURRENCY_VALUES.clear();
            CURRENCY_SINGULAR_NAMES.clear();
            CURRENCY_PLURAL_NAMES.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : objectMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                String currencyId = entry.getKey().getPath();
                JsonObject json = entry.getValue().getAsJsonObject();

                try {
                    String singular = json.has("singular_name") ? json.get("singular_name").getAsString() : "Unit";
                    String plural = json.has("plural_name") ? json.get("plural_name").getAsString() : "Units";

                    CURRENCY_SINGULAR_NAMES.put(currencyId, singular);
                    CURRENCY_PLURAL_NAMES.put(currencyId, plural);

                    Map<Item, Integer> valuesMap = new HashMap<>();
                    if (json.has("items")) {
                        JsonObject itemsObj = json.getAsJsonObject("items");
                        for (Map.Entry<String, JsonElement> itemEntry : itemsObj.entrySet()) {
                            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemEntry.getKey()));
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                valuesMap.put(item, itemEntry.getValue().getAsInt());
                            }
                        }
                    }
                    CURRENCY_VALUES.put(currencyId, valuesMap);
                } catch (Exception e) {
                    CurrencyManager.LOGGER.error("Failed parsing currency profile: {}", entry.getKey(), e);
                }
            }
        }
    }

    public static class MappingLoader extends SimpleJsonResourceReloadListener {
        public MappingLoader() { super(GSON, "prices"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objectMap, ResourceManager mgr, ProfilerFiller profiler) {
            MOB_TO_CURRENCY.clear();
            MOB_BASE_PRICES.clear();
            MOB_ALLOW_BABIES.clear();
            MOB_ATTRIBUTES.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : objectMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                ResourceLocation fileId = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();

                try {
                    String path = fileId.getPath();
                    int splitIndex = path.indexOf('/');
                    if (splitIndex == -1) continue;

                    String targetModId = path.substring(0, splitIndex);
                    String targetEntityName = path.substring(splitIndex + 1);
                    if (targetEntityName.endsWith(".json")) {
                        targetEntityName = targetEntityName.substring(0, targetEntityName.length() - 5);
                    }
                    String reconstructedMobId = targetModId + ":" + targetEntityName;

                    String currencyTarget = json.get("currency_id").getAsString();
                    if (currencyTarget.contains(":")) {
                        currencyTarget = currencyTarget.substring(currencyTarget.indexOf(':') + 1);
                    }
                    int basePrice = json.has("base_price") ? json.get("base_price").getAsInt() : 0;

                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(reconstructedMobId));
                    if (type != null) {
                        MOB_TO_CURRENCY.put(type, currencyTarget);
                        MOB_BASE_PRICES.put(type, basePrice);

                        if (json.has("allow_babies")) {
                            MOB_ALLOW_BABIES.put(type, json.get("allow_babies").getAsBoolean());
                        }
                        
                        JsonElement attrArrayElem = null;
                        if (json.has("modifiers") && json.get("modifiers").isJsonArray()) {
                            attrArrayElem = json.get("modifiers");
                        } else if (json.has("attributes") && json.get("attributes").isJsonArray()) {
                            attrArrayElem = json.get("attributes");
                        } else if (json.has("scaling") && json.getAsJsonObject("scaling").has("attributes")) {
                            attrArrayElem = json.getAsJsonObject("scaling").get("attributes");
                        }

                        if (attrArrayElem != null && attrArrayElem.isJsonArray()) {
                            List<MobAttributeModifier> customList = new ArrayList<>();
                            for (JsonElement elem : attrArrayElem.getAsJsonArray()) {
                                JsonObject attrObj = elem.getAsJsonObject();
                                String attrKey = attrObj.has("key") ? attrObj.get("key").getAsString() : attrObj.get("attribute").getAsString();

                                double multPerUnit = 1.0;
                                if (attrObj.has("multiplierPerUnit")) {
                                    multPerUnit = attrObj.get("multiplierPerUnit").getAsDouble();
                                } else if (attrObj.has("multiplier_per_unit")) {
                                    multPerUnit = attrObj.get("multiplier_per_unit").getAsDouble();
                                }

                                customList.add(new MobAttributeModifier(
                                        attrKey,
                                        attrObj.has("type") ? attrObj.get("type").getAsString() : "linear",
                                        attrObj.has("baseline") ? attrObj.get("baseline").getAsDouble() : 0.0,
                                        multPerUnit,
                                        attrObj.has("threshold") ? attrObj.get("threshold").getAsDouble() : 0.0,
                                        attrObj.has("bonusAmount") ? attrObj.get("bonusAmount").getAsInt() : (attrObj.has("bonus_amount") ? attrObj.get("bonus_amount").getAsInt() : 0),
                                        attrObj.has("minMultiplier") ? attrObj.get("minMultiplier").getAsDouble() : (attrObj.has("min_multiplier") ? attrObj.get("min_multiplier").getAsDouble() : 0.1),
                                        attrObj.has("maxMultiplier") ? attrObj.get("maxMultiplier").getAsDouble() : (attrObj.has("max_multiplier") ? attrObj.get("max_multiplier").getAsDouble() : 10.0)
                                ));
                            }
                            MOB_ATTRIBUTES.put(type, customList);
                        }
                    }
                } catch (Exception e) {
                    CurrencyManager.LOGGER.error("Failed parsing folder-mapped price file: {}", fileId, e);
                }
            }
        }
    }

    public static void setClientCache(Map<String, Map<Item, Integer>> values,
                                      Map<String, String> singularNames,
                                      Map<String, String> pluralNames,
                                      Map<EntityType<?>, String> mobs,
                                      Map<EntityType<?>, Integer> prices,
                                      Map<EntityType<?>, Boolean> babies,
                                      Map<EntityType<?>, List<MobAttributeModifier>> attributes) {
        CLIENT_CURRENCY_VALUES.clear();
        CLIENT_CURRENCY_SINGULAR_NAMES.clear();
        CLIENT_CURRENCY_PLURAL_NAMES.clear();
        CLIENT_MOB_TO_CURRENCY.clear();
        CLIENT_MOB_BASE_PRICES.clear();
        CLIENT_MOB_ALLOW_BABIES.clear();
        CLIENT_MOB_ATTRIBUTES.clear();

        CLIENT_CURRENCY_VALUES.putAll(values);
        CLIENT_CURRENCY_SINGULAR_NAMES.putAll(singularNames);
        CLIENT_CURRENCY_PLURAL_NAMES.putAll(pluralNames);
        CLIENT_MOB_TO_CURRENCY.putAll(mobs);
        CLIENT_MOB_BASE_PRICES.putAll(prices);
        CLIENT_MOB_ALLOW_BABIES.putAll(babies);
        CLIENT_MOB_ATTRIBUTES.putAll(attributes);
    }

    public static com.github.mythicalmontie.mobmarket.network.SyncCurrencies createSyncPacket() {
        return new com.github.mythicalmontie.mobmarket.network.SyncCurrencies(
                CURRENCY_VALUES,
                CURRENCY_SINGULAR_NAMES,
                CURRENCY_PLURAL_NAMES,
                MOB_TO_CURRENCY,
                MOB_BASE_PRICES,
                MOB_ALLOW_BABIES,
                MOB_ATTRIBUTES
        );
    }

    public static boolean isMobConfigured(EntityType<?> mobType) {return !MOB_BASE_PRICES.isEmpty() ? MOB_BASE_PRICES.containsKey(mobType) : CLIENT_MOB_BASE_PRICES.containsKey(mobType);}

    public static int getBasePriceForMob(EntityType<?> mobType) {return !MOB_BASE_PRICES.isEmpty() ? MOB_BASE_PRICES.getOrDefault(mobType, 0) : CLIENT_MOB_BASE_PRICES.getOrDefault(mobType, 0);}

    public static boolean areBabiesEligible(EntityType<?> mobType) {
        boolean useServer = !MOB_BASE_PRICES.isEmpty();
        Map<EntityType<?>, Boolean> babyMap = useServer ? MOB_ALLOW_BABIES : CLIENT_MOB_ALLOW_BABIES;
        if (babyMap.containsKey(mobType)) {
            return babyMap.get(mobType);
        }
        return Config.babiesAllowed;
    }

    public static int calculateFinalPrice(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        double price = getBasePriceForMob(type);
        List<MobAttributeModifier> modifiers = null;

        if (price <= 0) {
            LOGGER.info("MobMarket Debug: Mob type {} is not configured or base price <= 0 (base: {})", type, price);
            return 0;
        }

        boolean useServer = !MOB_BASE_PRICES.isEmpty();
        Map<EntityType<?>, List<MobAttributeModifier>> attrMap = useServer ? MOB_ATTRIBUTES : CLIENT_MOB_ATTRIBUTES;
        if (attrMap.containsKey(type)) {
            modifiers = attrMap.get(type);
        }

        if (modifiers != null && !modifiers.isEmpty()) {
            for (MobAttributeModifier modifier : modifiers) {
                price = modifier.applyModifier(entity, price);
            }
        }

        for (String rawRule : Config.globalAttributes) {
            String[] parts = rawRule.split("\\|");
            if (parts.length >= 6) {
                try {
                    double baseline = Double.parseDouble(parts[2]);
                    double multiplierOrBonus = Double.parseDouble(parts[3]);
                    double minMult = Double.parseDouble(parts[4]);
                    double maxMult = Double.parseDouble(parts[5]);

                    MobAttributeModifier globalMod = new MobAttributeModifier(
                            parts[0], parts[1],
                            baseline,
                            multiplierOrBonus,
                            baseline,
                            (int) Math.round(multiplierOrBonus),
                            minMult,
                            maxMult
                    );
                    price = globalMod.applyModifier(entity, price);
                } catch (NumberFormatException e) {
                    LOGGER.error("Failed to parse global attribute config string: {}", rawRule, e);
                }
            }
        }

        int calculatedPrice = Math.max(1, (int) Math.round(price));
        LOGGER.info("MobMarket Debug: Calculated price for {} = raw price {}, final rounded = {}", type, price, calculatedPrice);


        return calculatedPrice;
    }

    public static String getCurrencyIdForMob(EntityType<?> mobType) {
        return !MOB_TO_CURRENCY.isEmpty() ? MOB_TO_CURRENCY.get(mobType) : CLIENT_MOB_TO_CURRENCY.get(mobType);
    }

    public static Map<Item, Integer> getCurrencyValuesMap(String currencyId) {
        if (currencyId == null) return Collections.emptyMap();
        String cleanId = currencyId.contains(":") ? currencyId.substring(currencyId.indexOf(':') + 1) : currencyId;
        return !CURRENCY_VALUES.isEmpty() ? CURRENCY_VALUES.get(cleanId) : CLIENT_CURRENCY_VALUES.get(cleanId);
    }

    public static String getCurrencyName(String currencyId, int count) {
        boolean useServer = !CURRENCY_SINGULAR_NAMES.isEmpty();
        Map<String, String> namesMap = (count == 1)
                ? (useServer ? CURRENCY_SINGULAR_NAMES : CLIENT_CURRENCY_SINGULAR_NAMES)
                : (useServer ? CURRENCY_PLURAL_NAMES : CLIENT_CURRENCY_PLURAL_NAMES);
        return namesMap.getOrDefault(currencyId, count == 1 ? "Unit" : "Units");
    }
}

