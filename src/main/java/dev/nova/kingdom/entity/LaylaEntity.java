
package dev.nova.kingdom.entity;

// @name: LaylaEntity
// @type: Java entity class (server-side logic)
// @location: src/main/java/dev/nova/kingdom/entity/LaylaEntity.java

import dev.nova.kingdom.ai.LaylaLearningSystem;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import net.minecraft.world.entity.player.Player;
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

        // Prevent Layla from naturally despawning.
        this.setPersistenceRequired();
    }

    // ==========================================
    // AI GOALS
    // ==========================================

    @Override
    protected void registerGoals() {

        // Swim when in water.
        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        // Wander when idle.
        this.goalSelector.addGoal(
                3,
                new WaterAvoidingRandomStrollGoal(this, 1.0)
        );

        // Look at nearby players.
        this.goalSelector.addGoal(
                4,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        // Look around naturally.
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
    // LEARNING SYSTEM
    // ==========================================

    @Override
    public void tick() {

        // Keep Minecraft's normal entity behavior.
        super.tick();

        // Learning actions must run on the server.
        if (this.level() instanceof ServerLevel serverLevel) {

            // The learning system handles its own
            // timing and practice requirements.
            LaylaLearningSystem.tickLayla(
                    this,
                    serverLevel
            );
        }
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

        // Change emotion based on affection.
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
