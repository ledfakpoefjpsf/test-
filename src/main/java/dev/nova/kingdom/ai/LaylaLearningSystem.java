
package dev.nova.kingdom.ai;

// @name: LaylaLearningSystem
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/

import dev.nova.kingdom.entity.LaylaEntity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LaylaLearningSystem {

    private static final Map<UUID, LaylaBrain> BRAINS =
            new HashMap<>();

    private static int ticks = 0;

    private LaylaLearningSystem() {
    }

    public static LaylaBrain getBrain(LaylaEntity layla) {
        return BRAINS.computeIfAbsent(
                layla.getUUID(),
                id -> new LaylaBrain()
        );
    }

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, blockEntity) -> {

                    if (!(world instanceof ServerLevel level)) {
                        return;
                    }

                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    LaylaBrain.Skill skill = classifyBlock(state);

                    if (skill == null) {
                        return;
                    }

                    AABB area = new AABB(pos).inflate(12.0);

                    for (LaylaEntity layla :
                            level.getEntitiesOfClass(
                                    LaylaEntity.class,
                                    area
                            )) {

                        LaylaBrain brain = getBrain(layla);

                        LaylaBrain.Stage before =
                                brain.getStage(skill);

                        brain.observe(skill, 3);

                        LaylaBrain.Stage after =
                                brain.getStage(skill);

                        if (before != after) {
                            serverPlayer.displayClientMessage(
                                    Component.literal(
                                            "Layla: I'm learning "
                                            + skill.name().toLowerCase()
                                            + "! (" + after.name() + ")"
                                    ),
                                    false
                            );
                        }
                    }
                }
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;

            if (ticks % 40 != 0) {
                return;
            }

            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {

                    AABB area =
                            player.getBoundingBox().inflate(16.0);

                    for (LaylaEntity layla :
                            level.getEntitiesOfClass(
                                    LaylaEntity.class,
                                    area
                            )) {

                        LaylaBrain brain = getBrain(layla);

                        observeHeldTool(
                                brain,
                                player.getMainHandItem()
                        );

                        brain.chooseGoal(
                                true,
                                false,
                                false
                        );
                    }
                }
            }
        });
    }

    private static LaylaBrain.Skill classifyBlock(
            BlockState state
    ) {
        if (state.is(BlockTags.LOGS)) {
            return LaylaBrain.Skill.WOODCUTTING;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return LaylaBrain.Skill.MINING;
        }

        if (state.is(BlockTags.CROPS)) {
            return LaylaBrain.Skill.FARMING;
        }

        return LaylaBrain.Skill.BUILDING;
    }

    private static void observeHeldTool(
            LaylaBrain brain,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        if (stack.getItem() instanceof AxeItem) {
            brain.observe(LaylaBrain.Skill.WOODCUTTING, 1);
        } else if (stack.getItem() instanceof PickaxeItem) {
            brain.observe(LaylaBrain.Skill.MINING, 1);
        } else if (stack.getItem() instanceof HoeItem) {
            brain.observe(LaylaBrain.Skill.FARMING, 1);
        } else if (stack.getItem() instanceof SwordItem) {
            brain.observe(LaylaBrain.Skill.COMBAT, 1);
        }
    }
}
