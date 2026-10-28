package com.xtrip.golemgarage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import com.xtrip.golemgarage.item.PartItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One recipe per part shape that crafts that shape from any datapack material: the pattern's "M" slots
 * take the material's tagged item (all the same material), the other keys are the shape's usual extras.
 */
public record DatapackPartRecipe(String shape, List<String> pattern, Map<String, Ingredient> key) implements CraftingRecipe {
    public static final MapCodec<DatapackPartRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("shape").forGetter(DatapackPartRecipe::shape),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(DatapackPartRecipe::pattern),
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(DatapackPartRecipe::key)
    ).apply(i, DatapackPartRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DatapackPartRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DatapackPartRecipe::shape,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DatapackPartRecipe::pattern,
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, Ingredient.CONTENTS_STREAM_CODEC),
            DatapackPartRecipe::key,
            DatapackPartRecipe::new);

    public static final RecipeSerializer<DatapackPartRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<DatapackPartRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DatapackPartRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    /** The pattern with empty outer rows and columns removed. */
    private String[] trimmed() {
        int top = 0, bottom = pattern.size() - 1, left = Integer.MAX_VALUE, right = -1;
        while (top <= bottom && pattern.get(top).isBlank()) {
            top++;
        }
        while (bottom >= top && pattern.get(bottom).isBlank()) {
            bottom--;
        }
        for (int r = top; r <= bottom; r++) {
            String row = pattern.get(r);
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) != ' ') {
                    left = Math.min(left, c);
                    right = Math.max(right, c);
                }
            }
        }
        String[] out = new String[Math.max(0, bottom - top + 1)];
        for (int r = top; r <= bottom; r++) {
            String row = pattern.get(r) + " ".repeat(4);
            out[r - top] = row.substring(left, right + 1);
        }
        return out;
    }

    @Nullable
    private GolemMaterial match(CraftingInput input, boolean mirror) {
        String[] p = trimmed();
        if (p.length == 0 || input.height() != p.length || input.width() != p[0].length()) {
            return null;
        }
        GolemMaterial found = null;
        for (int y = 0; y < p.length; y++) {
            for (int x = 0; x < p[0].length(); x++) {
                char ch = p[y].charAt(mirror ? p[0].length() - 1 - x : x);
                ItemStack s = input.getItem(x, y);
                if (ch == ' ') {
                    if (!s.isEmpty()) {
                        return null;
                    }
                } else if (ch == 'M') {
                    GolemMaterial m = materialOf(s);
                    if (m == null || (found != null && found != m)) {
                        return null;
                    }
                    found = m;
                } else {
                    Ingredient ing = key.get(String.valueOf(ch));
                    if (ing == null || !ing.test(s)) {
                        return null;
                    }
                }
            }
        }
        return found;
    }

    /** Which datapack material an item stands for, if any. */
    @Nullable
    private static GolemMaterial materialOf(ItemStack s) {
        if (s.isEmpty()) {
            return null;
        }
        for (GolemMaterials.Extra x : GolemMaterials.extras().values()) {
            if (x.material().matches(s)) {
                return x.material();
            }
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return match(input, false) != null || match(input, true) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        GolemMaterial m = match(input, false);
        if (m == null) {
            m = match(input, true);
        }
        PartShape s = PartShapes.get(shape);
        if (m == null || s == null) {
            return ItemStack.EMPTY;
        }
        return PartItem.create(s, m);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= 3 && h >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
}
