package com.github.mythicalmontie.mobmarket.datagen;

import com.github.mythicalmontie.mobmarket.MobMarket;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        for (Map.Entry<String, RegistryObject<Block>> entry : MobMarket.MARKET_BLOCKS.entrySet()) {
            String compositeKey = entry.getKey();
            Block marketPostBlock = entry.getValue().get();
            String blockName = ForgeRegistries.BLOCKS.getKey(marketPostBlock).getPath();

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

            String slabItemName = woodName + "_slab";
            String fenceItemName = woodName + "_fence";

            if (woodName.equals("crimson") || woodName.equals("warped")) {
                slabItemName = woodName + "_slab";
                fenceItemName = woodName + "_fence";
            } else if (woodName.equals("bamboo")) {
                slabItemName = "bamboo_mosaic_slab";
                fenceItemName = "bamboo_fence";
            }

            Item slabItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(woodNamespace, slabItemName));
            if (slabItem == null || slabItem == Items.AIR) {
                slabItem = Items.OAK_SLAB;
            }

            Item fenceItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(woodNamespace, fenceItemName));
            if (fenceItem == null || fenceItem == Items.AIR) {
                fenceItem = Items.OAK_FENCE;
            }

            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, marketPostBlock)
                    .define('S', slabItem)
                    .define('C', Tags.Items.COBBLESTONE)
                    .define('P', Items.PAPER)
                    .define('F', fenceItem)
                    .pattern(" S ")
                    .pattern("CPC")
                    .pattern(" F ")
                    .unlockedBy("has_paper", has(Items.PAPER))
                    .save(writer, new ResourceLocation(MobMarket.MODID, blockName));
        }
    }
}