package com.xtrip.golemgarage.data;

import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.RuleCardItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/** A punched rule card plus blank cards gives that many copies; the original stays in the grid. */
public class CardCopyRecipe extends CustomRecipe {
    public static final RecipeSerializer<CardCopyRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(CardCopyRecipe::new);

    public CardCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static int[] count(CraftingInput input) {
        int punched = 0, blank = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (!s.is(ModItems.RULE_CARD.get())) {
                return new int[]{-1, -1};
            }
            if (RuleCardItem.isPunched(s)) {
                punched++;
            } else {
                blank++;
            }
        }
        return new int[]{punched, blank};
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int[] c = count(input);
        return c[0] == 1 && c[1] >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int[] c = count(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (RuleCardItem.isPunched(s)) {
                ItemStack out = new ItemStack(ModItems.RULE_CARD.get(), c[1]);
                out.set(DataComponents.CUSTOM_DATA, s.get(DataComponents.CUSTOM_DATA));
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rest = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            if (RuleCardItem.isPunched(input.getItem(i))) {
                rest.set(i, input.getItem(i).copyWithCount(1));
            }
        }
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
