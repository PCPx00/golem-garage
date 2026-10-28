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

/** "Drive a worn-out golem into the Garage with its materials nearby and it comes out like new." */
public class GarageRepairCategory implements IRecipeCategory<GarageRepairCategory.Repair> {
    public static final RecipeType<Repair> TYPE = RecipeType.create(GolemGarage.MOD_ID, "garage_repair", Repair.class);

    public record Repair(ItemStack broken, List<ItemStack> materials, ItemStack repaired) {
    }

    private final IDrawable icon;

    public GarageRepairCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(ModItems.GARAGE_CONTROLLER.get());
    }

    @Override
    public RecipeType<Repair> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.golemgarage.garage_repair");
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
        return 56;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Repair r, IFocusGroup focuses) {
        b.addInputSlot(4, 6).setStandardSlotBackground().addItemStack(r.broken());
        if (!r.materials().isEmpty()) {
            b.addInputSlot(30, 6).setStandardSlotBackground().addItemStacks(r.materials());
        }
        b.addOutputSlot(104, 6).setOutputSlotBackground().addItemStack(r.repaired());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Repair r, IFocusGroup focuses) {
        b.addAnimatedRecipeArrowWidget(60).setPosition(66, 6);
        b.addText(Component.translatable("jei.golemgarage.garage_repair.note"), 126, 22)
                .setPosition(2, 30).setColor(0xFF606060);
    }
}
