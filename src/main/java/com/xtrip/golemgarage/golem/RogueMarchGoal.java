package com.xtrip.golemgarage.golem;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** A rogue with no target marches on the base its raid is heading for. */
public class RogueMarchGoal extends Goal {
    private final GolemEntity golem;

    public RogueMarchGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** A point up to len blocks along the way to t (paths can only be planned a short way ahead). */
    public static net.minecraft.world.phys.Vec3 hop(net.minecraft.world.entity.Mob mob, BlockPos t, double len) {
        net.minecraft.world.phys.Vec3 to = net.minecraft.world.phys.Vec3.atBottomCenterOf(t);
        net.minecraft.world.phys.Vec3 d = to.subtract(mob.position());
        if (d.horizontalDistance() <= len) {
            return to;
        }
        net.minecraft.world.phys.Vec3 p = mob.position().add(d.multiply(1, 0, 1).normalize().scale(len));
        int y = mob.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(p.x), (int) Math.floor(p.z));
        return new net.minecraft.world.phys.Vec3(p.x, y, p.z);
    }

    @Override
    public boolean canUse() {
        BlockPos t = golem.raidTarget();
        return golem.isRogue() && golem.getTarget() == null && !golem.isPassenger() && t != null
                && golem.blockPosition().distSqr(t) > 36;
    }

    @Override
    public void tick() {
        BlockPos t = golem.raidTarget();
        if (t != null && (golem.getNavigation().isDone() || golem.tickCount % 40 == 0)) {
            net.minecraft.world.phys.Vec3 step = hop(golem, t, 12);
            golem.getNavigation().moveTo(step.x, step.y, step.z, 1.0);
        }
    }
}
