package com.xtrip.golemgarage.compat.jei;

import com.xtrip.golemgarage.GolemGarage;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The Scrap Burner: what each fuel is worth in FE, and how fast it comes out. */
public class BurnerCategory implements IRecipeCategory<BurnerCategory.Fuel> {
    public static final RecipeType<Fuel> TYPE = RecipeType.create(GolemGarage.MOD_ID, "scrap_burner", Fuel.class);

    /** Fuels that burn for the same time, with the FE they give in total and per tick. */
    public record Fuel(List<ItemStack> items, long total, int rate, int seconds) {
    }

    private final IDrawable icon;

    public BurnerCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(com.xtrip.golemgarage.power.PowerBlocks.BURNER_ITEM.get());
    }

    @Override
    public RecipeType<Fuel> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.golemgarage.scrap_burner");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Fuel r, IFocusGroup focuses) {
        b.addInputSlot(4, 9).setStandardSlotBackground().addItemStacks(r.items());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Fuel r, IFocusGroup focuses) {
        b.addAnimatedRecipeFlame(r.seconds() * 20).setPosition(26, 10);
        b.addText(Component.translatable("jei.golemgarage.scrap_burner.total", String.format("%,d", r.total())), 90, 12)
                .setPosition(44, 4).setColor(0xFF404040);
        b.addText(Component.translatable("jei.golemgarage.scrap_burner.rate", r.rate(), r.seconds()), 90, 12)
                .setPosition(44, 18).setColor(0xFF707070);
    }
}
