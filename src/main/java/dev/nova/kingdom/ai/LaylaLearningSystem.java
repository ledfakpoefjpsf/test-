
package dev.nova.kingdom.ai;

// @name: LaylaLearningSystem
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/LaylaLearningSystem.java

import dev.nova.kingdom.entity.LaylaEntity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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

    private LaylaLearningSystem() {
    }

    // Each Layla has her own learning progress.
    public static LaylaBrain getBrain(LaylaEntity layla) {
        return BRAINS.computeIfAbsent(
                layla.getUUID(),
                id -> new LaylaBrain()
        );
    }

    // Each Layla has her own temporary inventory.
    public static SimpleContainer getInventory(LaylaEntity layla) {
        return INVENTORIES.computeIfAbsent(
                layla.getUUID(),
                id -> new SimpleContainer(18)
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

                    // Layla learns when she watches the player
                    // break logs near her.
                    if (state.is(BlockTags.LOGS)) {

                        AABB area = new AABB(pos).inflate(12.0);

                        for (LaylaEntity layla :
                                level.getEntitiesOfClass(
                                        LaylaEntity.class,
                                        area
                                )) {

                            LaylaBrain brain = getBrain(layla);

                            int previousXp =
                                    brain.getExperience(
                                            LaylaBrain.Skill.WOODCUTTING
                                    );

                            brain.observe(
                                    LaylaBrain.Skill.WOODCUTTING,
                                    5
                            );

                            int currentXp =
                                    brain.getExperience(
                                            LaylaBrain.Skill.WOODCUTTING
                                    );

                            if (previousXp < 20 && currentXp >= 20) {
                                serverPlayer.displayClientMessage(
                                        Component.literal(
                                                "Layla: I think I can chop trees now!"
                                        ),
                                        false
                                );
                            }
                        }
                    }
                }
        );
    }

    // Called by Layla's entity tick once connected.
    public static void tickLayla(
            LaylaEntity layla,
            ServerLevel level
    ) {

        // Run once every two seconds.
        if (layla.tickCount % 40 != 0) {
            return;
        }

        LaylaBrain brain = getBrain(layla);

        if (!brain.canPractice(
                LaylaBrain.Skill.WOODCUTTING
        )) {
            return;
        }

        practiceWoodcutting(level, layla, brain);
    }

    private static void practiceWoodcutting(
            ServerLevel level,
            LaylaEntity layla,
            LaylaBrain brain
    ) {

        SimpleContainer inventory = getInventory(layla);

        if (!inventory.canAddItem(
                new ItemStack(Items.OAK_LOG)
        )) {
            return;
        }

        BlockPos origin = layla.blockPosition();
        BlockPos target = null;

        // Look for logs close to Layla.
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-4, -1, -4),
                origin.offset(4, 3, 4)
        )) {

            BlockState state = level.getBlockState(pos);

            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            // Prefer natural trees rather than logs
            // used in houses or other structures.
            boolean leavesNearby = false;

            for (BlockPos nearby : BlockPos.betweenClosed(
                    pos.offset(-2, 0, -2),
                    pos.offset(2, 3, 2)
            )) {

                if (level.getBlockState(nearby)
                        .is(BlockTags.LEAVES)) {

                    leavesNearby = true;
                    break;
                }
            }

            if (leavesNearby) {
                target = pos.immutable();
                break;
            }
        }

        if (target == null) {
            return;
        }

        double distance = layla.distanceToSqr(
                target.getX() + 0.5,
                target.getY() + 0.5,
                target.getZ() + 0.5
        );

        // Walk toward the tree.
        if (distance > 9.0) {
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

        // Get normal block drops.
        var drops = Block.getDrops(
                state,
                level,
                target,
                level.getBlockEntity(target)
        );

        if (drops.isEmpty()) {
            return;
        }

        // Break the log without spawning duplicate drops.
        if (!level.destroyBlock(target, false)) {
            return;
        }

        for (ItemStack stack : drops) {

            ItemStack remainder =
                    inventory.addItem(stack.copy());

            // Drop items that do not fit.
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

    // Transfer collected logs to a player.
    public static void giveWoodToPlayer(
            LaylaEntity layla,
            ServerPlayer player
    ) {

        SimpleContainer inventory = getInventory(layla);

        for (int slot = 0;
             slot < inventory.getContainerSize();
             slot++) {

            ItemStack stack = inventory.getItem(slot);

            // FIX: ItemTags.LOGS instead of
            // BlockTags.LOGS.asItemTag()
            if (stack.isEmpty()
                    || !stack.is(ItemTags.LOGS)) {
                continue;
            }

            ItemStack removed =
                    inventory.removeItem(
                            slot,
                            stack.getCount()
                    );

            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
        }
    }
}
