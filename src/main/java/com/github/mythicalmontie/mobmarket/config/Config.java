package com.github.mythicalmontie.mobmarket.config;

import com.github.mythicalmontie.mobmarket.MobMarket;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;


@Mod.EventBusSubscriber(modid = MobMarket.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue TARGET_CHECK_TICK = BUILDER
            .comment("The global server time of day (in ticks) when all market blocks scan for entities. 18000 is Midnight, 0 is Sunrise, 6000 is Noon.")
            .defineInRange("checkTick",18000,0,18000);

    public static final ForgeConfigSpec.IntValue MARKET_SALE_CHANCE = BUILDER
            .comment("The chance that a detected mob will successfully be sold each night. 100 = Always, 0 = Never.")
            .defineInRange("saleChance",100,0,100);

    public static final ForgeConfigSpec.BooleanValue SEND_SALE_MESSAGES = BUILDER
            .comment("If true, sends a chat message to the market post owner or whoever can access it (team/public) notifying them of how many mobs were sold and for how much")
            .define("sendSaleMessages",true);

    public static ForgeConfigSpec.ConfigValue<List<?extends String>> INVARIANT_PLURALS = BUILDER
            .comment("A list of Mod IDs or specific registry mob ids that should never receive a plural suffix on invoices.\n" +
                    "If you input a Mod ID namespace, every single creature from that mod will bypass pluralization.\n" +
                    "Example: 'alexsmobs', 'naturalist', 'minecraft:sheep', 'deer', 'enderman'")
            .defineListAllowEmpty(java.util.List.of("invariantPlurals"),
                    () -> java.util.List.of("minecraft:sheep", "minecraft:squid", "minecraft:cod", "minecraft:salmon", "minecraft:pufferfish", "minecraft:tropical_fish", "minecraft:mooshroom", "minecraft:enderman"),
                    obj -> obj instanceof String);

    public static ForgeConfigSpec.BooleanValue BABIES_ALLOWED = BUILDER
            .comment("If true, babies can be sold for any allowed mob. Can be overridden by individual mob files.")
            .define("babiesAllowed",true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GLOBAL_ATTRIBUTES = BUILDER
            .comment()
            .defineListAllowEmpty(List.of("globalAttributes"),
            () -> List.of(""),
                    obj -> obj instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int targetCheckTick;
    public static int saleChance;
    public static boolean sendSaleMessages;
    public static List<? extends String> invariantPlurals;
    public static boolean babiesAllowed;
    public static List<? extends String> globalAttributes;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        targetCheckTick = TARGET_CHECK_TICK.get();
        saleChance = MARKET_SALE_CHANCE.get();
        sendSaleMessages = SEND_SALE_MESSAGES.get();
        invariantPlurals = INVARIANT_PLURALS.get();
        babiesAllowed = BABIES_ALLOWED.get();
        globalAttributes = GLOBAL_ATTRIBUTES.get();
    }
}
