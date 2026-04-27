package com.absolutenarwhal.largeoreveins.integration;

import com.absolutenarwhal.largeoreveins.LargeOreVeins;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfigLoader;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@mezz.jei.api.JeiPlugin
public class LargeOreVeinsJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(LargeOreVeins.MOD_ID, "jei_plugin");

    private static IJeiRuntime jeiRuntime;
    private static final List<OreVeinConfig> addedRecipes = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() { return PLUGIN_ID; }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        onConfigsReloaded(); // populate recipes once runtime is ready
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new OreVeinCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {}

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {}

    public static void onConfigsReloaded() {
        if (jeiRuntime == null) return;

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();

        // hide previously added recipes
        if (!addedRecipes.isEmpty()) {
            recipeManager.hideRecipes(OreVeinCategory.TYPE, addedRecipes);
            addedRecipes.clear();
        }

        List<OreVeinConfig> veins = OreVeinConfigLoader.getAllVeins().values().stream()
            .filter(OreVeinConfig::enabled)
            .toList();

        if (!veins.isEmpty()) {
            recipeManager.addRecipes(OreVeinCategory.TYPE, veins);
            addedRecipes.addAll(veins);
        }
    }
}
