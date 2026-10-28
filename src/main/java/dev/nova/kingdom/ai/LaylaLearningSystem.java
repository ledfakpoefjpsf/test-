
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LaylaLearningSystem {

    private static final Map<UUID, LaylaBrain> BRAINS =
            new HashMap<>();

    private static final Map<UUID, SimpleContainer> INVENTORIES =
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

    public static SimpleContainer getInventory(LaylaEntity layla) {
        return INVENTORIES.computeIfAbsent(
                layla.getUUID(),
                id -> new SimpleContainer(18)
        );
    }

    public static void register() {

        // Learn when the player breaks logs.
        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, blockEntity) -> {

                    if (!(world instanceof ServerLevel level)
                            || !(player instanceof ServerPlayer owner)
                            || !state.is(BlockTags.LOGS)) {
                        return;
                    }

                    AABB area = new AABB(pos).inflate(12);

                    for (LaylaEntity layla :
                            level.getEntitiesOfClass(
                                    LaylaEntity.class,
                                    area
                            )) {

                        LaylaBrain brain = getBrain(layla);

                        int oldXp = brain.getExperience(
                                LaylaBrain.Skill.WOODCUTTING
                        );

                        brain.observe(
                                LaylaBrain.Skill.WOODCUTTING,
                                5
                        );

                        if (oldXp < 20
                                && brain.getExperience(
                                        LaylaBrain.Skill.WOODCUTTING
                                ) >= 20) {

                            owner.displayClientMessage(
                                    Component.literal(
                                            "Layla: I think I can try chopping trees!"
                                    ),
                                    false
                            );
                        }
                    }
                }
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            ticks++;

            // One update every two seconds.
            if (ticks % 40 != 0) {
                return;
            }

            for (ServerLevel level : server.getAllLevels()) {

                for (ServerPlayer player : level.players()) {

                    AABB area =
                            player.getBoundingBox().inflate(20);

                    for (LaylaEntity layla :
                            level.getEntitiesOfClass(
                                    LaylaEntity.class,
                                    area
                            )) {

                        LaylaBrain brain = getBrain(layla);

                        double distance =
                                layla.distanceToSqr(player);

                        // Stay near the player.
                        if (distance > 100) {
                            layla.getNavigation().moveTo(
                                    player,
                                    1.15
                            );
                            continue;
                        }

                        if (distance > 25) {
                            layla.getNavigation().moveTo(
                                    player,
                                    1.1
                            );
                        }

                        // Practice gathering after learning.
                        if (brain.canPractice(
                                LaylaBrain.Skill.WOODCUTTING
                        )) {
                            practiceWoodcutting(
                                    level,
                                    layla,
                                    brain
                            );
                        }
                    }
                }
            }
        });
    }

    private static void practiceWoodcutting(
            ServerLevel level,
            LaylaEntity layla,
            LaylaBrain brain
    ) {

        SimpleContainer inventory = getInventory(layla);

        // Do not continue when storage is full.
        if (!inventory.canAddItem(
                new ItemStack(Items.OAK_LOG)
        )) {
            return;
        }

        BlockPos origin = layla.blockPosition();
        BlockPos target = null;

        // Search close to Layla, not across the whole world.
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        origin.offset(-4, -1, -4),
                        origin.offset(4, 3, 4)
                )) {

            BlockState state = level.getBlockState(pos);

            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            // Only target logs with leaves nearby.
            // This reduces the chance of dismantling buildings.
            boolean hasLeaves = false;

            for (BlockPos nearby :
                    BlockPos.betweenClosed(
                            pos.offset(-2, 0, -2),
                            pos.offset(2, 3, 2)
                    )) {

                if (level.getBlockState(nearby)
                        .is(BlockTags.LEAVES)) {
                    hasLeaves = true;
                    break;
                }
            }

            if (hasLeaves) {
                target = pos.immutable();
                break;
            }
        }

        if (target == null) {
            return;
        }

        // Walk toward the tree first.
        double distance = layla.distanceToSqr(
                target.getX() + 0.5,
                target.getY() + 0.5,
                target.getZ() + 0.5
        );

        if (distance > 9) {
            layla.getNavigation().moveTo(
                    target.getX() + 0.5,
                    target.getY(),
                    target.getZ() + 0.5,
                    1.0
            );
            return;
        }

        BlockState state = level.getBlockState(target);

        if (!state.is(BlockTags.LOGS)) {
            return;
        }

        // Collect the block's normal drops.
        // This initial version doesn't simulate tool enchantments.
        var drops = net.minecraft.world.level.block.Block.getDrops(
                state,
                level,
                target,
                level.getBlockEntity(target)
        );

        if (drops.isEmpty()) {
            return;
        }

        if (!level.destroyBlock(target, false)) {
            return;
        }

        for (ItemStack stack : drops) {
            ItemStack remainder = inventory.addItem(stack.copy());

            if (!remainder.isEmpty()) {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                layla.getX(),
                                layla.getY() + 0.5,
                                layla.getZ(),
                                remainder
                        )
                );
            }
        }

        brain.recordPractice(
                LaylaBrain.Skill.WOODCUTTING,
                true
        );
    }

    public static void giveWoodToPlayer(
            LaylaEntity layla,
            ServerPlayer player
    ) {

        SimpleContainer inventory = getInventory(layla);

        for (int slot = 0;
             slot < inventory.getContainerSize();
             slot++) {

            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty()
                    || !stack.is(BlockTags.LOGS.asItemTag())) {
                continue;
            }

            ItemStack transferred =
                    inventory.removeItem(slot, stack.getCount());

            if (!player.getInventory().add(transferred)) {
                player.drop(transferred, false);
            }
        }
    }
}
