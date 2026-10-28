
package dev.nova.kingdom.entity;

// @name: LaylaEntity
// @type: server-side Java entity class
// @location: src/main/java/dev/nova/kingdom/entity/LaylaEntity.java

import dev.nova.kingdom.ai.LaylaLearningSystem;
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
import net.minecraft.world.SimpleMenuProvider;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
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
            EntityType<? extends LaylaEntity> type,
            Level level
    ) {
        super(type, level);
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

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        // Follow / independent work / chill behavior.
        LaylaTaskController.tick(this, level);

        // Maintain tools when Layla is allowed to work.
        if (LaylaTaskController.canWork(this, level)) {

            LaylaToolLearning.tick(this);

            // Keep the existing woodcutting system.
            if (LaylaTaskController.getTask(this)
                    == LaylaTask.WOODCUTTING) {

                LaylaLearningSystem.tickLayla(
                        this,
                        level
                );
            }
        }
    }

    // ==========================================
    // SHIFT + RIGHT-CLICK INVENTORY
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

        // Server-side inventory opening.
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = serverPlayer.level();

        ServerPlayer owner =
                LaylaTaskController.getOwner(this, level);

        // First player becomes Layla's owner.
        if (owner == null) {

            LaylaTaskController.setOwner(
                    this,
                    serverPlayer
            );

        } else if (!owner.getUUID()
                .equals(serverPlayer.getUUID())) {

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla: Only my owner can open my inventory!"
                    ),
                    false
            );

            return InteractionResult.SUCCESS;
        }

        openLaylaInventory(serverPlayer);

        return InteractionResult.SUCCESS;
    }

    // ==========================================
    // LAYLA INVENTORY MENU
    // ==========================================

    private void openLaylaInventory(ServerPlayer player) {

        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInventory, menuPlayer) ->
                                new ChestMenu(
                                        MenuType.GENERIC_9x2,
                                        containerId,
                                        playerInventory,
                                        LaylaLearningSystem.getInventory(this),
                                        2
                                ),
                        Component.literal("Layla's Inventory")
                )
        );
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

        this.entityData.set(
                EMOTION_STATE,
                updated < 30 ? 2 : 0
        );
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
    public boolean removeWhenFarAway(
            double distanceSquared
    ) {
        return false;
    }
}
