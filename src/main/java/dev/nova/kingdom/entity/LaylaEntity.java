
package dev.nova.kingdom.entity;

// @name: LaylaEntity
// @type: Java entity class (server-side)
// @location: src/main/java/dev/nova/kingdom/entity/LaylaEntity.java

import dev.nova.kingdom.ai.LaylaLearningSystem;
import dev.nova.kingdom.ai.LaylaMode;
import dev.nova.kingdom.ai.LaylaTask;
import dev.nova.kingdom.ai.LaylaTaskController;
import dev.nova.kingdom.ai.LaylaToolLearning;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class LaylaEntity extends KingdomVillagerEntity {

    // ==========================================
    // AFFECTION AND EMOTIONS
    // ==========================================

    private static final EntityDataAccessor<Integer> AFFECTION =
            SynchedEntityData.defineId(
                    LaylaEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> EMOTION_STATE =
            SynchedEntityData.defineId(
                    LaylaEntity.class,
                    EntityDataSerializers.INT
            );

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public LaylaEntity(
            EntityType<? extends LaylaEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    // ==========================================
    // AI GOALS
    // ==========================================

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                3,
                new WaterAvoidingRandomStrollGoal(this, 1.0)
        );

        this.goalSelector.addGoal(
                4,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        this.goalSelector.addGoal(
                5,
                new RandomLookAroundGoal(this)
        );
    }

    // ==========================================
    // SYNCHRONIZED DATA
    // ==========================================

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);

        builder.define(AFFECTION, 100);
        builder.define(EMOTION_STATE, 0);
    }

    // ==========================================
    // ATTRIBUTES
    // ==========================================

    public static AttributeSupplier.Builder createLaylaAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    // ==========================================
    // MAIN AI TICK
    // ==========================================

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Update Layla's movement orders.
        LaylaTaskController.tick(
                this,
                serverLevel
        );

        // Maintain tools based on the assigned task.
        // The tool system runs its own cooldown.
        LaylaToolLearning.tick(this);

        // Perform woodcutting only when allowed.
        if (LaylaTaskController.canWork(this, serverLevel)
                && LaylaTaskController.getTask(this)
                        == LaylaTask.WOODCUTTING) {

            LaylaLearningSystem.tickLayla(
                    this,
                    serverLevel
            );
        }
    }

    // ==========================================
    // PLAYER INTERACTIONS
    // ==========================================

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        if (hand != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }

        // Interactions that change AI state run
        // on the server only.
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = serverPlayer.level();

        ServerPlayer currentOwner =
                LaylaTaskController.getOwner(
                        this,
                        level
                );

        // First player to interact becomes owner
        // for the current server session.
        if (currentOwner == null) {
            LaylaTaskController.setOwner(
                    this,
                    serverPlayer
            );
        } else if (!currentOwner.getUUID()
                .equals(serverPlayer.getUUID())) {

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla: I'm following someone else."
                    ),
                    false
            );

            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getMainHandItem();

        LaylaMode mode = null;
        LaylaTask task = null;

        // ======================================
        // MOVEMENT ORDERS
        // ======================================

        if (held.is(Items.COMPASS)) {
            mode = LaylaMode.FOLLOW;

        } else if (held.is(Items.CLOCK)) {
            mode = LaylaMode.FOLLOW_AND_WORK;

        } else if (held.is(Items.MAP)) {
            mode = LaylaMode.WORK_INDEPENDENTLY;

        } else if (held.is(Items.WHITE_BED)) {
            mode = LaylaMode.CHILL;
        }

        // ======================================
        // TASK ORDERS
        // ======================================

        else if (held.is(Items.OAK_LOG)) {
            task = LaylaTask.WOODCUTTING;

        } else if (held.is(Items.IRON_PICKAXE)) {
            task = LaylaTask.MINING;

        } else if (held.is(Items.WHEAT_SEEDS)) {
            task = LaylaTask.FARMING;

        } else if (held.is(Items.FISHING_ROD)) {
            task = LaylaTask.FISHING;

        } else if (held.is(Items.CRAFTING_TABLE)) {
            task = LaylaTask.CRAFTING;

        } else if (held.is(Items.BRICKS)) {
            task = LaylaTask.BUILDING;

        } else if (held.is(Items.TORCH)) {
            task = LaylaTask.EXPLORING;
        }

        // ======================================
        // APPLY ORDERS
        // ======================================

        if (mode != null) {

            LaylaTaskController.setMode(this, mode);

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla mode: " + mode.name()
                    ),
                    false
            );

        } else if (task != null) {

            LaylaTaskController.setTask(this, task);

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla task: " + task.name()
                    ),
                    false
            );

        } else if (held.isEmpty()) {

            // Empty hand: collect her stored logs.
            LaylaLearningSystem.giveWoodToPlayer(
                    this,
                    serverPlayer
            );

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla: Here are my collected logs!"
                    ),
                    false
            );

        } else {

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla: Use a compass, clock, map, "
                            + "bed, oak log, iron pickaxe, seeds, "
                            + "fishing rod, crafting table, bricks "
                            + "or torch to give me an order."
                    ),
                    false
            );
        }

        return InteractionResult.SUCCESS;
    }

    // ==========================================
    // AFFECTION SYSTEM
    // ==========================================

    public void adjustAffection(int amount) {

        int current = this.entityData.get(AFFECTION);

        int updated = Math.max(
                0,
                Math.min(200, current + amount)
        );

        this.entityData.set(AFFECTION, updated);

        if (updated < 30) {
            this.entityData.set(EMOTION_STATE, 2);
        } else {
            this.entityData.set(EMOTION_STATE, 0);
        }
    }

    public int getAffection() {
        return this.entityData.get(AFFECTION);
    }

    public int getEmotionState() {
        return this.entityData.get(EMOTION_STATE);
    }

    // ==========================================
    // DESPAWN PROTECTION
    // ==========================================

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
