package com.github.mythicalmontie.mobmarket;

import com.github.mythicalmontie.mobmarket.block.MobMarketBlock;
import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import com.github.mythicalmontie.mobmarket.blockentity.MobMarketGUI;
import com.github.mythicalmontie.mobmarket.client.MobMarketScreen;
import com.github.mythicalmontie.mobmarket.config.Config;
import com.github.mythicalmontie.mobmarket.network.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(MobMarket.MODID)
public class MobMarket
{
    public static final String MODID = "mobmarket";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Map<String, RegistryObject<Block>> MARKET_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> MARKET_ITEMS = new LinkedHashMap<>();

    static {
        String[] vanillaWoods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};
        for (String wood : vanillaWoods) {
            registerWoodVariant("minecraft", wood);
        }

        if (ModList.get() != null && ModList.get().isLoaded("biomesoplenty")) {
            String[] bopWoods = {"fir", "redwood", "mahogany", "jacaranda", "palm", "willow", "magic", "umbran", "dead", "hellbark"};
            for (String wood : bopWoods) {
                registerWoodVariant("biomesoplenty", wood);
            }
        }

        if (ModList.get() != null && ModList.get().isLoaded("biomeswevegone")) {
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
                        .sound(net.minecraft.world.level.block.SoundType.WOOD)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                )
        );
        RegistryObject<Item> itemReg = ITEMS.register(registryKey, () -> new BlockItem(blockReg.get(), new Item.Properties()));

        MARKET_BLOCKS.put(mapKey, blockReg);
        MARKET_ITEMS.put(mapKey, itemReg);
    }

    public static final RegistryObject<BlockEntityType<MobMarketBlockEntity>> MOB_MARKET_BE = BLOCK_ENTITIES.register("mob_market", () -> BlockEntityType.Builder.of(MobMarketBlockEntity::new, MARKET_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<MenuType<MobMarketGUI>> MOB_MARKET_MENU = MENUS.register("mob_market",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(MobMarketGUI::new));
    public static final RegistryObject<CreativeModeTab> MARKET_TAB = CREATIVE_MODE_TABS.register("market_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mobmarket.tab"))
            .icon(() -> MARKET_ITEMS.values().iterator().next().get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> itemObj : MARKET_ITEMS.values()) {
                    output.accept(itemObj.get());
                }
            }).build());

    public MobMarket()
    {

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "mobmarket-common.toml");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        ModMessages.register();
        modEventBus.register(this);
        MinecraftForge.EVENT_BUS.register(com.github.mythicalmontie.mobmarket.config.ConfigLifecycleHandler.class);
        MinecraftForge.EVENT_BUS.register(com.github.mythicalmontie.mobmarket.network.ForgeEventSubscriber.class);
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(
                        MobMarket.MOB_MARKET_MENU.get(),
                        MobMarketScreen::new
                );

                for (RegistryObject<Block> blockObj : MARKET_BLOCKS.values()) {
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                            blockObj.get(),
                            net.minecraft.client.renderer.RenderType.cutout()
                    );
                }
            });
        }
    }
}
