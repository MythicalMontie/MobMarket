package com.github.mythicalmontie.mobmarket.datagen;

import com.github.mythicalmontie.mobmarket.MobMarket;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class ModBlockStateProvider extends BlockStateProvider {
    private final ExistingFileHelper fileHelper;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MobMarket.MODID, exFileHelper);
        this.fileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        for (Map.Entry<String, RegistryObject<Block>> entry : MobMarket.MARKET_BLOCKS.entrySet()) {
            String compositeKey = entry.getKey();
            Block block = entry.getValue().get();
            String blockName = ForgeRegistries.BLOCKS.getKey(block).getPath();

            String woodNamespace = "minecraft";
            String woodName = compositeKey;

            if (compositeKey.contains("_") && !compositeKey.equals("dark_oak")) {
                int firstUnderscore = compositeKey.indexOf('_');
                String potentialModId = compositeKey.substring(0, firstUnderscore);
                String potentialWood = compositeKey.substring(firstUnderscore + 1);

                if (!potentialModId.equals("crimson") && !potentialModId.equals("warped")) {
                    woodNamespace = potentialModId;
                    woodName = potentialWood;
                }
            }

            String planksName = woodName + "_planks";
            String logSideName = woodName + "_log";
            String logTopName = woodName + "_log_top";

            String planksFolderPath = "block/";
            String logFolderPath = "block/";

            if (woodName.equals("crimson") || woodName.equals("warped")) {
                planksName = woodName + "_planks";
                logSideName = woodName + "_stem";
                logTopName = woodName + "_stem_top";
            } else if (woodName.equals("bamboo")) {
                planksName = "bamboo_planks";
                logSideName = "bamboo_block";
                logTopName = "bamboo_block";
            }

            if (woodNamespace.equals("biomeswevegone")) {
                planksFolderPath = "block/" + woodName + "/";
                logFolderPath = "block/" + woodName + "/";

                planksName = "planks";

                if (woodName.equals("florus")) {
                    logSideName = "stem";
                    logTopName = "stem_top";
                } else {
                    logSideName = "log";
                    logTopName = "log_top";
                }
            }

            ResourceLocation planksTex = new ResourceLocation(woodNamespace, planksFolderPath + planksName);
            ResourceLocation logSideTex = new ResourceLocation(woodNamespace, logFolderPath + logSideName);
            ResourceLocation logTopTex = new ResourceLocation(woodNamespace, logFolderPath + logTopName);

            if (!woodNamespace.equals("minecraft") && !woodNamespace.equals(MobMarket.MODID)) {
                fileHelper.trackGenerated(planksTex, net.minecraft.server.packs.PackType.CLIENT_RESOURCES, ".png", "textures");
                fileHelper.trackGenerated(logSideTex, net.minecraft.server.packs.PackType.CLIENT_RESOURCES, ".png", "textures");
                fileHelper.trackGenerated(logTopTex, net.minecraft.server.packs.PackType.CLIENT_RESOURCES, ".png", "textures");
            }

            BlockModelBuilder floorModel = models().withExistingParent(blockName,
                            new ResourceLocation(MobMarket.MODID, "block/mob_market"))
                    .texture("planks", planksTex).texture("log_side", logSideTex).texture("log_top", logTopTex)
                    .texture("paper", new ResourceLocation(MobMarket.MODID, "block/paper"))
                    .texture("stone", new ResourceLocation(MobMarket.MODID, "block/stone")).texture("particle", planksTex);

            BlockModelBuilder wallModel = models().withExistingParent(blockName + "_wall",
                            new ResourceLocation(MobMarket.MODID, "block/mob_market_wall"))
                    .texture("planks", planksTex).texture("log_side", logSideTex).texture("log_top", logTopTex)
                    .texture("paper", new ResourceLocation(MobMarket.MODID, "block/paper"))
                    .texture("stone", new ResourceLocation(MobMarket.MODID, "block/stone")).texture("particle", planksTex);

            VariantBlockStateBuilder builder = getVariantBuilder(block);

            for (net.minecraft.core.Direction dir : HorizontalDirectionalBlock.FACING.getPossibleValues()) {
                builder.partialState()
                        .with(HorizontalDirectionalBlock.FACING, dir)
                        .with(com.github.mythicalmontie.mobmarket.block.MobMarketBlock.FACE, AttachFace.FLOOR)
                        .modelForState().modelFile(floorModel)
                        .rotationY((int) dir.toYRot())
                        .addModel();

                builder.partialState()
                        .with(HorizontalDirectionalBlock.FACING, dir)
                        .with(com.github.mythicalmontie.mobmarket.block.MobMarketBlock.FACE, AttachFace.WALL)
                        .modelForState().modelFile(wallModel)
                        .rotationY((int) dir.toYRot())
                        .addModel();

                builder.partialState()
                        .with(HorizontalDirectionalBlock.FACING, dir)
                        .with(com.github.mythicalmontie.mobmarket.block.MobMarketBlock.FACE, AttachFace.CEILING)
                        .modelForState().modelFile(wallModel)
                        .rotationY((int) dir.toYRot())
                        .addModel();
            }

            itemModels().withExistingParent(blockName, new ResourceLocation(MobMarket.MODID, "block/" + blockName));
        }
    }
}