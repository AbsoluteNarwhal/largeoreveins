package com.absolutenarwhal.largeoreveins.integration;

import com.absolutenarwhal.largeoreveins.LargeOreVeins;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import com.absolutenarwhal.largeoreveins.veindata.VeinInfo;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OreVeinCategory implements IRecipeCategory<OreVeinConfig> {

    public static final RecipeType<OreVeinConfig> TYPE = RecipeType.create(
        LargeOreVeins.MOD_ID, "ore_vein", OreVeinConfig.class
    );

    private final IDrawable icon;
    private final IGuiHelper guiHelper;

    public OreVeinCategory(IJeiHelpers helpers) {
        this.guiHelper = helpers.getGuiHelper();
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(Items.DIAMOND_ORE)
        );
    }

    @Override
    public RecipeType<OreVeinConfig> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("category.largeoreveins.ore_vein");
    }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 85;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OreVeinConfig vein, IFocusGroup focuses) {
        int x = 0;
        List<Block> blocks = new ArrayList<>();

        for (ResourceLocation oreBlockId : vein.replaceBlocks().values()) {
            Block block = BuiltInRegistries.BLOCK.get(oreBlockId);
            if (blocks.contains(block)) continue;
            blocks.add(block);

            if (block != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, x, 0)
                    .addItemStack(new ItemStack(block.asItem()));
                x += 18;
            }
        }
    }

    @Override
    public void draw(OreVeinConfig vein, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int y = 20;
        int color = 0xff404040;
        graphics.drawString(mc.font, VeinInfo.getName(vein).getString(), 0, y, color, false);
        y += 10;
        graphics.drawString(mc.font, "Dimension: " + getDimensionName(mc, vein.dimension()), 0, y, color, false);
        y += 10;
        graphics.drawString(mc.font, "Y Range: " + vein.minY() + " to " + vein.maxY(), 0, y, color, false);
        y += 10;
        graphics.drawString(mc.font, "Weight: " + vein.defaultWeight() + " (" + VeinInfo.formatWeight(vein) + ")", 0, y, color, false);

        List<String> commonBiomes = vein.biomeWeights().entrySet().stream()
            .filter(e -> e.getValue() > vein.defaultWeight())
            .map(Map.Entry::getKey)
            .toList();

        if (!commonBiomes.isEmpty()) {
            y += 10;
            graphics.drawString(mc.font, "Commonly Found In:", 0, y, color, false);
            for (String biome : commonBiomes) {
                y += 10;
                graphics.drawString(mc.font, "  " + getBiomeName(mc, biome), 0, y, color, false);
            }
        }
    }

    private String getDimensionName(Minecraft mc, ResourceLocation dimensionId) {
        String translationKey = "dimension." + dimensionId.getNamespace() + "." + dimensionId.getPath();
        Component translated = Component.translatable(translationKey);
        String result = translated.getString();
        return result.equals(translationKey) ? dimensionId.toString() : result;
    }

    private String getBiomeName(Minecraft mc, String biomeKey) {
        if (biomeKey.startsWith("#")) {
            // tag
            ResourceLocation tagId = ResourceLocation.parse(biomeKey.substring(1));
            String translationKey = "biome_tag." + tagId.getNamespace() + "." + tagId.getPath().replace("/", ".");
            Component translated = Component.translatable(translationKey);
            String result = translated.getString();
            if (!result.equals(translationKey)) return result;

            // not localised
            return biomeKey;
        }

        // biome ID
        ResourceLocation biomeId = ResourceLocation.parse(biomeKey);
        String translationKey = "biome." + biomeId.getNamespace() + "." + biomeId.getPath();
        Component translated = Component.translatable(translationKey);
        String result = translated.getString();
        return result.equals(translationKey) ? biomeId.toString() : result;
    }
}
