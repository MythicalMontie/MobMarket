package com.github.mythicalmontie.mobmarket;

import com.github.mythicalmontie.mobmarket.block.MobMarketBlock;
import com.github.mythicalmontie.mobmarket.config.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(MobMarket.MODID)
public class MobMarket
{
    public static final String MODID = "mobmarket";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Map<String, RegistryObject<Block>> MARKET_BLOCKS = new HashMap<>();
    public static final Map<String, RegistryObject<Item>> MARKET_ITEMS = new HashMap<>();

    static {
        String[] vanillaWoods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};
        for (String wood : vanillaWoods) {
            registerWoodVariant("minecraft", wood);
        }

        if ((ModList.get() != null && ModList.get().isLoaded("biomesoplenty")) || net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            String[] bopWoods = {"fir", "redwood", "mahogany", "jacaranda", "palm", "willow", "magic", "umbran", "dead", "hellbark"};
            for (String wood : bopWoods) {
                registerWoodVariant("biomesoplenty", wood);
            }
        }

        if ((ModList.get() != null && ModList.get().isLoaded("biomeswevegone")) || net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            String[] bwgWoods = {
                    "aspen", "baobab", "blue_enchanted", "cika", "cypress",
                    "ebony", "fir", "florus", "green_enchanted", "holly", "ironwood",
                    "jacaranda", "mahogany", "maple", "palm", "pine",
                    "rainbow_eucalyptus", "redwood", "sakura", "skyris", "white_mangrove", "willow",
                    "witch_hazel", "zelkova"
            };
            for (String wood : bwgWoods) {
                registerWoodVariant("biomeswevegone", wood);
            }
        }
    }

    public static void registerWoodVariant(String namespace, String woodName) {
        String mapKey = namespace.equals("minecraft") ? woodName : namespace + "_" + woodName;
        if (MARKET_BLOCKS.containsKey(mapKey)) return;

        String registryKey = mapKey + "_mob_market";

        RegistryObject<Block> blockReg = BLOCKS.register(registryKey, () ->
                new MobMarketBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(3.0f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                )
        );
        RegistryObject<Item> itemReg = ITEMS.register(registryKey, () -> new BlockItem(blockReg.get(), new Item.Properties()));

        MARKET_BLOCKS.put(mapKey, blockReg);
        MARKET_ITEMS.put(mapKey, itemReg);
    }

    public MobMarket()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "mobmarket-common.toml");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }
    }
}
