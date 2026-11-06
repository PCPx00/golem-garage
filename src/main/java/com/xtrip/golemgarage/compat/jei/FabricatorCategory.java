package com.xtrip.golemgarage.compat.jei;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.item.ModItems;
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

/** The Parts Fabricator: pick a shape, feed it a stack of one material, and it welds the part. */
public class FabricatorCategory implements IRecipeCategory<FabricatorCategory.Weld> {
    public static final RecipeType<Weld> TYPE = RecipeType.create(GolemGarage.MOD_ID, "fabricator", Weld.class);

    public record Weld(List<ItemStack> material, ItemStack part) {
    }

    private final IDrawable icon;

    public FabricatorCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(ModItems.FABRICATOR.get());
    }

    @Override
    public RecipeType<Weld> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.golemgarage.fabricator");
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
        return 50;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Weld r, IFocusGroup focuses) {
        b.addInputSlot(10, 6).setStandardSlotBackground().addItemStacks(r.material());
        b.addOutputSlot(100, 6).setOutputSlotBackground().addItemStack(r.part());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Weld r, IFocusGroup focuses) {
        b.addAnimatedRecipeArrowWidget(100).setPosition(50, 6);
        b.addText(Component.translatable("jei.golemgarage.fabricator.note"), 126, 20)
                .setPosition(2, 28).setColor(0xFF606060);
    }
}
