package com.xtrip.golemgarage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xtrip.golemgarage.Config;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.GolemRegistry;
import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.GolemText;
import com.xtrip.golemgarage.item.GolemItem;
import com.xtrip.golemgarage.item.GolemLinkerItem;
import com.xtrip.golemgarage.item.ModDataComponents;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * /golemgarage (and /gg): look after your golems from anywhere.
 * <p>Players: list, find, recall, rename, stop, resume. Operators: give, repair, owner, count, purge, debug.</p>
 * Golems are named by their name, by their number in /golemgarage list, or "all".
 */
public final class GolemCommands {
    private static final SimpleCommandExceptionType NO_GOLEM =
            new SimpleCommandExceptionType(Component.translatable("golemgarage.cmd.no_golem"));
    private static final Map<UUID, Long> lastRecall = new HashMap<>();

    /** Builds, rules and the parts each preset uses, for /golemgarage give. */
    private static final Map<String, Preset> PRESETS = new java.util.LinkedHashMap<>();

    private record Preset(String head, String torso, String arms, String legs, String core,
                          java.util.function.Supplier<List<GolemRule>> rules) {
    }

    static {
        PRESETS.put("standard", new Preset("sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core",
                GolemRule::followerPreset));
        PRESETS.put("guard", new Preset("scope_head", "plated_chest", "hammer_arms", "stub_legs", "redstone_core",
                GolemRule::guardPreset));
        PRESETS.put("hauler", new Preset("sentry_head", "cargo_barrel", "grabber_claws", "wheel_legs", "redstone_core",
                GolemRule::haulerPreset));
        PRESETS.put("harvester", new Preset("lantern_head", "cargo_barrel", "scythe_arms", "strider_legs", "solar_core",
                GolemRule::harvesterPreset));
        PRESETS.put("medic", new Preset("sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core",
                GolemRule::medicPreset));
        PRESETS.put("night_guard", new Preset("lantern_head", "plated_chest", "golem_fists", "tread_legs", "soul_core",
                GolemRule::nightGuardPreset));
    }

    private GolemCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var root = d.register(build("golemgarage"));
        d.register(Commands.literal("gg").requires(root.getRequirement()).redirect(root));
    }

    // =================================================================== tree
    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        SuggestionProvider<CommandSourceStack> golems = (ctx, b) -> {
            List<String> out = new ArrayList<>();
            out.add("all");
            List<GolemRegistry.Entry> mine = owned(ctx.getSource());
            for (int i = 0; i < mine.size(); i++) {
                out.add(String.valueOf(i + 1));
                if (!mine.get(i).name().isEmpty()) {
                    out.add(StringArgumentType.escapeIfRequired(mine.get(i).name()));
                }
            }
            return SharedSuggestionProvider.suggest(out, b);
        };
        SuggestionProvider<CommandSourceStack> materials = (ctx, b) -> SharedSuggestionProvider.suggest(
                GolemMaterials.CRAFTABLE.stream().map(GolemMaterial::id), b);
        SuggestionProvider<CommandSourceStack> presets = (ctx, b) -> SharedSuggestionProvider.suggest(PRESETS.keySet(), b);
        SuggestionProvider<CommandSourceStack> colors = (ctx, b) -> SharedSuggestionProvider.suggest(
                java.util.Arrays.stream(DyeColor.values()).map(DyeColor::getName), b);

        return Commands.literal(name)
                .then(Commands.literal("list").requires(GolemCommands::playerCommands)
                        .executes(ctx -> list(ctx.getSource(), ctx.getSource().getPlayerOrException().getUUID(), null))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    var profile = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
                                    return list(ctx.getSource(), profile.getId(), profile.getName());
                                })))
                .then(Commands.literal("find").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .executes(ctx -> find(ctx.getSource(), StringArgumentType.getString(ctx, "golem")))))
                .then(Commands.literal("recall").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .executes(ctx -> recall(ctx.getSource(), StringArgumentType.getString(ctx, "golem")))))
                .then(Commands.literal("rename").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> rename(ctx.getSource(), StringArgumentType.getString(ctx, "golem"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("mode").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .then(Commands.argument("mode", StringArgumentType.word()).suggests((c, b) ->
                                                SharedSuggestionProvider.suggest(java.util.Arrays.stream(
                                                        com.xtrip.golemgarage.golem.GolemMode.values()).map(
                                                        com.xtrip.golemgarage.golem.GolemMode::id), b))
                                        .executes(ctx -> mode(ctx.getSource(), StringArgumentType.getString(ctx, "golem"),
                                                StringArgumentType.getString(ctx, "mode"))))))
                .then(Commands.literal("stop").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .executes(ctx -> pause(ctx.getSource(), StringArgumentType.getString(ctx, "golem"), true))))
                .then(Commands.literal("resume").requires(GolemCommands::playerCommands)
                        .then(Commands.argument("golem", StringArgumentType.string()).suggests(golems)
                                .executes(ctx -> pause(ctx.getSource(), StringArgumentType.getString(ctx, "golem"), false))))
                // ---- operators
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("golem")
                                        .then(Commands.argument("material", StringArgumentType.word()).suggests(materials)
                                                .executes(ctx -> giveGolem(ctx, "standard"))
                                                .then(Commands.argument("preset", StringArgumentType.word()).suggests(presets)
                                                        .executes(ctx -> giveGolem(ctx, StringArgumentType.getString(ctx, "preset"))))))
                                .then(Commands.literal("cub")
                                        .executes(ctx -> giveCub(ctx, "pink"))
                                        .then(Commands.argument("color", StringArgumentType.word()).suggests(colors)
                                                .executes(ctx -> giveCub(ctx, StringArgumentType.getString(ctx, "color")))))))
                .then(Commands.literal("repair").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("golems", EntityArgument.entities())
                                .executes(ctx -> repair(ctx.getSource(), EntityArgument.getEntities(ctx, "golems")))))
                .then(Commands.literal("owner").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("golems", EntityArgument.entities())
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> owner(ctx.getSource(), EntityArgument.getEntities(ctx, "golems"),
                                                GameProfileArgument.getGameProfiles(ctx, "player").iterator().next())))))
                .then(Commands.literal("count").requires(s -> s.hasPermission(2))
                        .executes(ctx -> count(ctx.getSource())))
                .then(Commands.literal("purge").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> purge(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player").iterator().next()))))
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("golem", EntityArgument.entity())
                                .executes(ctx -> debug(ctx.getSource(), EntityArgument.getEntity(ctx, "golem")))));
    }

    private static boolean playerCommands(CommandSourceStack s) {
        return s.hasPermission(2) || Config.get(Config.PLAYER_COMMANDS::get, true);
    }

    // =================================================================== helpers
    private static List<GolemRegistry.Entry> owned(CommandSourceStack s) {
        ServerPlayer p = s.getPlayer();
        if (p == null) {
            return List.of();
        }
        return GolemRegistry.get(s.getServer()).forOwner(p.getUUID());
    }

    /** The caller's golems matching "all", a list number, or a name (case-insensitive). */
    private static List<GolemRegistry.Entry> pick(CommandSourceStack s, String which) throws CommandSyntaxException {
        s.getPlayerOrException();
        List<GolemRegistry.Entry> mine = owned(s);
        if (which.equalsIgnoreCase("all")) {
            if (mine.isEmpty()) {
                throw NO_GOLEM.create();
            }
            return mine;
        }
        try {
            int i = Integer.parseInt(which);
            if (i >= 1 && i <= mine.size()) {
                return List.of(mine.get(i - 1));
            }
        } catch (NumberFormatException ignored) {
            // not a number: try names
        }
        List<GolemRegistry.Entry> out = new ArrayList<>();
        for (GolemRegistry.Entry e : mine) {
            if (e.name().equalsIgnoreCase(which)) {
                out.add(e);
            }
        }
        if (out.isEmpty()) {
            throw NO_GOLEM.create();
        }
        return out;
    }

    @org.jetbrains.annotations.Nullable
    private static GolemEntity loaded(MinecraftServer server, GolemRegistry.Entry e) {
        ServerLevel level = server.getLevel(e.dimension());
        if (level != null && level.getEntity(e.golem()) instanceof GolemEntity g && g.isAlive()) {
            return g;
        }
        return null;
    }

    private static Component label(GolemRegistry.Entry e, int index) {
        return Component.literal("#" + index + " ").withStyle(ChatFormatting.GRAY)
                .append(e.name().isEmpty() ? Component.translatable("entity.golemgarage.golem") : Component.literal(e.name()))
                .withStyle(ChatFormatting.WHITE);
    }

    private static Component where(GolemRegistry.Entry e, boolean op) {
        BlockPos p = e.pos();
        String coords = p.getX() + " " + p.getY() + " " + p.getZ();
        String dim = e.dimension().location().getPath();
        MutableComponent c = Component.literal("[" + coords + ", " + dim + "]").withStyle(ChatFormatting.AQUA);
        if (op) {
            String tp = "/execute in " + e.dimension().location() + " run tp @s " + coords;
            return c.withStyle(st -> st.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("golemgarage.cmd.click_tp"))));
        }
        return c.withStyle(st -> st.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click"))));
    }

    /** Runs {@code action} on the golem, loading its chunk for a moment if needed. */
    private static void withLoaded(MinecraftServer server, GolemRegistry.Entry e, Consumer<GolemEntity> action, Runnable missing) {
        GolemEntity g = loaded(server, e);
        if (g != null) {
            action.accept(g);
            return;
        }
        ServerLevel level = server.getLevel(e.dimension());
        if (level == null) {
            missing.run();
            return;
        }
        ChunkPos cp = new ChunkPos(e.pos());
        boolean wasForced = level.getForcedChunks().contains(cp.toLong());
        level.setChunkForced(cp.x, cp.z, true);
        com.xtrip.golemgarage.ModEvents.later(server, 100, tick -> {
            GolemEntity found = loaded(server, e);
            if (found == null && tick < 100) {
                return false; // keep waiting for the entities to load
            }
            if (!wasForced) {
                level.setChunkForced(cp.x, cp.z, false);
            }
            if (found != null) {
                action.accept(found);
            } else {
                GolemRegistry.get(server).remove(e.golem()); // it isn't there any more
                missing.run();
            }
            return true;
        });
    }

    // =================================================================== player commands
    private static int list(CommandSourceStack s, UUID owner, @org.jetbrains.annotations.Nullable String ownerName) {
        MinecraftServer server = s.getServer();
        List<GolemRegistry.Entry> golems = GolemRegistry.get(server).forOwner(owner);
        if (golems.isEmpty()) {
            s.sendSuccess(() -> Component.translatable("golemgarage.cmd.list_none"), false);
            return 0;
        }
        s.sendSuccess(() -> ownerName == null ? Component.translatable("golemgarage.cmd.list_title", golems.size())
                .withStyle(ChatFormatting.GOLD) : Component.translatable("golemgarage.cmd.list_title_other", ownerName,
                golems.size()).withStyle(ChatFormatting.GOLD), false);
        boolean op = s.hasPermission(2);
        for (int i = 0; i < golems.size(); i++) {
            GolemRegistry.Entry e = golems.get(i);
            GolemEntity g = loaded(server, e);
            MutableComponent line = Component.empty().append(label(e, i + 1)).append(" ").append(where(e, op));
            if (g != null) {
                int cap = g.readSyncedCapacity();
                line.append(Component.literal("  " + Math.round(g.getHealth()) + "/" + Math.round(g.getMaxHealth()) + " hp")
                        .withStyle(ChatFormatting.RED));
                if (cap > 0) {
                    line.append(Component.literal("  " + g.getPowerPercent() + "%").withStyle(ChatFormatting.BLUE));
                }
                line.append(Component.literal("  ")).append(GolemText.show(g.readSyncedStatus()).copy()
                        .withStyle(GolemText.isProblem(g.readSyncedStatus()) ? ChatFormatting.RED : ChatFormatting.GRAY));
            } else {
                line.append(Component.literal("  ")).append(Component.translatable("golemgarage.cmd.not_loaded")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (e.group() > 0) {
                line.append(Component.literal("  G" + e.group()).withStyle(ChatFormatting.YELLOW));
            }
            s.sendSuccess(() -> line, false);
        }
        return golems.size();
    }

    private static int find(CommandSourceStack s, String which) throws CommandSyntaxException {
        ServerPlayer player = s.getPlayerOrException();
        List<GolemRegistry.Entry> found = pick(s, which);
        for (GolemRegistry.Entry e : found) {
            GolemEntity g = loaded(s.getServer(), e);
            MutableComponent msg = Component.translatable("golemgarage.cmd.find", label(e, owned(s).indexOf(e) + 1), where(e, s.hasPermission(2)));
            if (e.dimension() == player.level().dimension()) {
                Vec3 d = Vec3.atCenterOf(e.pos()).subtract(player.position());
                msg.append(" ").append(Component.translatable("golemgarage.cmd.find_dist", Math.round(d.horizontalDistance()),
                        compass(d)).withStyle(ChatFormatting.GRAY));
            }
            if (g != null) {
                g.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            }
            s.sendSuccess(() -> msg, false);
        }
        return found.size();
    }

    private static String compass(Vec3 d) {
        String[] names = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        double yaw = Math.toDegrees(Math.atan2(-d.x, d.z));
        int i = Math.floorMod((int) Math.round(yaw / 45.0), 8);
        return names[i];
    }

    private static int recall(CommandSourceStack s, String which) throws CommandSyntaxException {
        ServerPlayer player = s.getPlayerOrException();
        int cooldown = Config.get(Config.RECALL_COOLDOWN_SECONDS::get, 60);
        long now = s.getServer().getTickCount();
        Long last = lastRecall.get(player.getUUID());
        if (!s.hasPermission(2) && last != null && now - last < cooldown * 20L) {
            long left = (cooldown * 20L - (now - last)) / 20 + 1;
            s.sendFailure(Component.translatable("golemgarage.cmd.recall_wait", left));
            return 0;
        }
        List<GolemRegistry.Entry> found = pick(s, which);
        lastRecall.put(player.getUUID(), now);
        for (GolemRegistry.Entry e : found) {
            withLoaded(s.getServer(), e, g -> {
                g.getNavigation().stop();
                if (g.level() == player.level()) {
                    g.teleportTo(player.getX(), player.getY(), player.getZ());
                } else {
                    g.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(player.serverLevel(),
                            player.position(), Vec3.ZERO, player.getYRot(), 0f,
                            net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                }
                player.sendSystemMessage(Component.translatable("golemgarage.cmd.recalled",
                        e.name().isEmpty() ? Component.translatable("entity.golemgarage.golem") : Component.literal(e.name()))
                        .withStyle(ChatFormatting.GREEN));
            }, () -> player.sendSystemMessage(Component.translatable("golemgarage.cmd.recall_lost").withStyle(ChatFormatting.RED)));
        }
        return found.size();
    }

    private static int rename(CommandSourceStack s, String which, String name) throws CommandSyntaxException {
        List<GolemRegistry.Entry> found = pick(s, which);
        if (found.size() != 1) {
            s.sendFailure(Component.translatable("golemgarage.cmd.one_golem"));
            return 0;
        }
        String clean = name.length() > 32 ? name.substring(0, 32) : name;
        withLoaded(s.getServer(), found.get(0), g -> {
            g.setCustomName(Component.literal(clean));
            GolemRegistry.get(s.getServer()).update(g);
            s.sendSuccess(() -> Component.translatable("golemgarage.cmd.renamed", clean).withStyle(ChatFormatting.GREEN), false);
        }, () -> s.sendFailure(Component.translatable("golemgarage.cmd.recall_lost")));
        return 1;
    }

    private static int pause(CommandSourceStack s, String which, boolean stop) throws CommandSyntaxException {
        List<GolemRegistry.Entry> found = pick(s, which);
        for (GolemRegistry.Entry e : found) {
            withLoaded(s.getServer(), e, g -> g.setPaused(stop), () -> {
            });
        }
        s.sendSuccess(() -> Component.translatable(stop ? "golemgarage.cmd.stopped" : "golemgarage.cmd.resumed", found.size())
                .withStyle(ChatFormatting.GREEN), false);
        return found.size();
    }

    private static int mode(CommandSourceStack s, String which, String modeName) throws CommandSyntaxException {
        com.xtrip.golemgarage.golem.GolemMode mode = null;
        for (var m : com.xtrip.golemgarage.golem.GolemMode.values()) {
            if (m.id().equalsIgnoreCase(modeName)) {
                mode = m;
            }
        }
        if (mode == null) {
            s.sendFailure(Component.translatable("golemgarage.cmd.bad_mode"));
            return 0;
        }
        final var chosen = mode;
        List<GolemRegistry.Entry> found = pick(s, which);
        for (GolemRegistry.Entry e : found) {
            withLoaded(s.getServer(), e, g -> g.setMode(chosen), () -> {
            });
        }
        s.sendSuccess(() -> Component.translatable("golemgarage.cmd.mode_set", found.size(), chosen.title())
                .withStyle(ChatFormatting.GREEN), false);
        return found.size();
    }

    // =================================================================== operator commands
    private static int giveGolem(CommandContext<CommandSourceStack> ctx, String presetName) throws CommandSyntaxException {
        String matId = StringArgumentType.getString(ctx, "material").toLowerCase(Locale.ROOT);
        GolemMaterial mat = GolemMaterials.get(matId);
        Preset preset = PRESETS.get(presetName.toLowerCase(Locale.ROOT));
        if (mat == null || preset == null) {
            ctx.getSource().sendFailure(Component.translatable("golemgarage.cmd.bad_give"));
            return 0;
        }
        CompoundTag tag = GolemData.preset(mat, preset.head(), preset.torso(), preset.arms(), preset.legs(), preset.core());
        tag.put("Rules", GolemRule.toTag(preset.rules().get()).get("Rules"));
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            ItemStack stack = GolemItem.stackFromTag(tag.copy());
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("golemgarage.cmd.gave_golem", mat.displayName(), presetName,
                targets.size()), true);
        return targets.size();
    }

    private static int giveCub(CommandContext<CommandSourceStack> ctx, String colorName) throws CommandSyntaxException {
        DyeColor color = DyeColor.byName(colorName.toLowerCase(Locale.ROOT), null);
        if (color == null) {
            ctx.getSource().sendFailure(Component.translatable("golemgarage.cmd.bad_give"));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            ItemStack stack = new ItemStack(ModItems.CUB.get());
            stack.set(ModDataComponents.COLOR.get(), color.getId());
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("golemgarage.cmd.gave_cub", targets.size()), true);
        return targets.size();
    }

    private static List<GolemEntity> golemsOf(Collection<? extends Entity> entities) throws CommandSyntaxException {
        List<GolemEntity> out = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof GolemEntity g) {
                out.add(g);
            }
        }
        if (out.isEmpty()) {
            throw NO_GOLEM.create();
        }
        return out;
    }

    private static int repair(CommandSourceStack s, Collection<? extends Entity> entities) throws CommandSyntaxException {
        List<GolemEntity> golems = golemsOf(entities);
        golems.forEach(GolemEntity::repairFully);
        s.sendSuccess(() -> Component.translatable("golemgarage.cmd.repaired", golems.size()), true);
        return golems.size();
    }

    private static int owner(CommandSourceStack s, Collection<? extends Entity> entities, com.mojang.authlib.GameProfile to)
            throws CommandSyntaxException {
        List<GolemEntity> golems = golemsOf(entities);
        for (GolemEntity g : golems) {
            g.setOwnerUuid(to.getId());
            GolemRegistry.get(s.getServer()).update(g);
        }
        s.sendSuccess(() -> Component.translatable("golemgarage.cmd.owner", golems.size(), to.getName()), true);
        return golems.size();
    }

    private static int count(CommandSourceStack s) {
        MinecraftServer server = s.getServer();
        Map<UUID, Integer> per = new HashMap<>();
        List<GolemRegistry.Entry> all = GolemRegistry.get(server).all();
        for (GolemRegistry.Entry e : all) {
            per.merge(e.owner() == null ? new UUID(0, 0) : e.owner(), 1, Integer::sum);
        }
        int loaded = 0;
        for (ServerLevel l : server.getAllLevels()) {
            loaded += l.getEntities(com.xtrip.golemgarage.entity.ModEntities.GOLEM.get(), g -> true).size();
        }
        final int loadedNow = loaded;
        s.sendSuccess(() -> Component.translatable("golemgarage.cmd.count", all.size(), loadedNow).withStyle(ChatFormatting.GOLD), false);
        per.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(en -> {
            String who = en.getKey().getLeastSignificantBits() == 0 && en.getKey().getMostSignificantBits() == 0
                    ? "-" : server.getProfileCache() != null
                    ? server.getProfileCache().get(en.getKey()).map(com.mojang.authlib.GameProfile::getName).orElse(en.getKey().toString())
                    : en.getKey().toString();
            s.sendSuccess(() -> Component.literal("  " + who + ": " + en.getValue()), false);
        });
        return all.size();
    }

    private static int purge(CommandSourceStack s, com.mojang.authlib.GameProfile who) {
        MinecraftServer server = s.getServer();
        GolemRegistry registry = GolemRegistry.get(server);
        int n = 0;
        for (GolemRegistry.Entry e : registry.forOwner(who.getId())) {
            GolemEntity g = loaded(server, e);
            if (g != null) {
                g.discard();
                registry.remove(e.golem());
            } else {
                registry.markPurged(e.golem());
            }
            n++;
        }
        final int total = n;
        s.sendSuccess(() -> Component.translatable("golemgarage.cmd.purged", total, who.getName()), true);
        return n;
    }

    private static int debug(CommandSourceStack s, Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof GolemEntity g)) {
            throw NO_GOLEM.create();
        }
        var st = g.stats();
        String owner = g.getOwnerUuid() == null ? "-" : s.getServer().getProfileCache() != null
                ? s.getServer().getProfileCache().get(g.getOwnerUuid()).map(com.mojang.authlib.GameProfile::getName)
                .orElse(g.getOwnerUuid().toString()) : g.getOwnerUuid().toString();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("== " + g.getName().getString() + " (" + g.getUUID() + ")").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("owner " + owner + ", group " + g.getGroup() + ", mode " + g.getMode().id()
                + (g.isPaused() ? ", PAUSED" : "")));
        lines.add(Component.literal(String.format(Locale.ROOT, "health %.0f/%.0f, power %d/%d, armor %d, attack %.1f, speed %.3f",
                g.getHealth(), g.getMaxHealth(), g.readSyncedPower(), g.readSyncedCapacity(), g.getArmorValue(), st.attack, st.speed)));
        g.getEquipped().forEach((slot, p) -> lines.add(Component.literal("  " + slot.id() + ": " + p.shape().id() + " / "
                + p.materialId() + "  " + p.durability() + "/" + p.maxDurability() + (p.oxidation() > 0 ? " ox" + p.oxidation() : ""))));
        lines.add(Component.literal("modules: " + g.getModules()));
        for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
            BlockPos p = g.getLink(m);
            if (p != null) {
                lines.add(Component.literal("  link " + m.name().toLowerCase(Locale.ROOT) + ": " + p.toShortString()));
            }
        }
        List<GolemRule> rules = g.getRules();
        for (int i = 0; i < rules.size(); i++) {
            lines.add(Component.literal("  rule " + (i + 1) + (i == g.readActiveRule() ? " *" : "") + ": " + rules.get(i).describe())
                    .withStyle(i == g.readActiveRule() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        lines.add(Component.literal("now: ").append(GolemText.show(g.readSyncedStatus())));
        for (String l : g.readSyncedLog()) {
            lines.add(Component.literal("  - ").append(GolemText.show(l)).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (Component c : lines) {
            s.sendSuccess(() -> c, false);
        }
        return 1;
    }
}
