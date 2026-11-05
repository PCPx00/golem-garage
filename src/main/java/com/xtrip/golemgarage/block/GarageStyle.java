package com.xtrip.golemgarage.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;

/**
 * How a Garage is built and how it looks once it transforms. Each style has its own windows (planned in the
 * build plan and placed by auto build) and its own skin.
 * <ul>
 *   <li>Wasteland: rusty corrugated iron, no windows. The classic.</li>
 *   <li>Showroom: clean painted steel with big glass windows on every wall and a glass roof, so you can see in.</li>
 *   <li>Bunker: blast concrete and dark steel with a row of narrow slit windows under the roof.</li>
 *   <li>Barn: red boards and white trim with a pair of windows down each side.</li>
 * </ul>
 */
public enum GarageStyle {
    WASTELAND(Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE),
    SHOWROOM(Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE),
    BUNKER(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE),
    BARN(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);

    /** What creative-mode auto build uses for walls, and for floor and roof. */
    public final Block creativeWall, creativeSolid;

    GarageStyle(Block wall, Block solid) {
        this.creativeWall = wall;
        this.creativeSolid = solid;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "golemgarage.style." + id();
    }

    public GarageStyle next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static GarageStyle of(int i) {
        return values()[Math.floorMod(i, values().length)];
    }

    /**
     * Whether a wall block is a window. d is the depth into the room (0 the controller's wall, depth + 1 the back),
     * l the position across (0 the middle), y the height above the floor; the room is w x depth x h inside.
     */
    public boolean window(int d, int l, int y, int w, int depth, int h) {
        int half = w / 2;
        boolean side = l == -half - 1 || l == half + 1;
        boolean front = d == 0, back = d == depth + 1;
        return switch (this) {
            case WASTELAND -> false;
            // Big bands of glass: every wall between the bottom and top rows, except the corners and the
            // controller's own column.
            case SHOWROOM -> y >= 1 && y <= h - 2 && (side ? d >= 1 && d <= depth : Math.abs(l) <= half)
                    && !(front && l == 0) && !(side && (d == 1 || d == depth) && depth > 3);
            // Narrow slits under the roof, every other block.
            case BUNKER -> y == h - 2 && h >= 4 && (side ? d % 2 == 0 && d >= 1 && d <= depth
                    : !front && !back ? false : Math.abs(l) <= half && Math.floorMod(l, 2) == 1);
            // Two tall windows down each side.
            case BARN -> side && y >= 1 && y <= Math.min(2, h - 2)
                    && (d == Math.max(1, depth / 3) || d == Math.max(1, depth - depth / 3) && depth >= 4);
        };
    }

    /** Glass roof between the emitters. */
    public boolean skylight() {
        return this == SHOWROOM;
    }
}
