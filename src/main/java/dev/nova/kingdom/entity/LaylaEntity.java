
package dev.nova.kingdom.entity;

// @name: LaylaEntity
// @type: server-side Java entity class
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class LaylaEntity extends KingdomVillagerEntity {

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

    public LaylaEntity(
            EntityType<? extends LaylaEntity> type,
            Level level
    ) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(
                3,
                new WaterAvoidingRandomStrollGoal(this, 1.0)
        );

        this.goalSelector.addGoal(
                4,
                new LookAtPlayerGoal(this, Player.class, 8.0F)
        );

        this.goalSelector.addGoal(
                5,
                new RandomLookAroundGoal(this)
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);

        builder.define(AFFECTION, 100);
        builder.define(EMOTION_STATE, 0);
    }

    public static AttributeSupplier.Builder createLaylaAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        LaylaTaskController.tick(this, level);

        if (!LaylaTaskController.canWork(this, level)) {
            return;
        }

        LaylaToolLearning.tick(this);

        if (LaylaTaskController.getTask(this)
                == LaylaTask.WOODCUTTING) {

            LaylaLearningSystem.tickLayla(this, level);
        }
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = serverPlayer.level();

        ServerPlayer owner =
                LaylaTaskController.getOwner(this, level);

        if (owner == null) {
            LaylaTaskController.setOwner(this, serverPlayer);
        } else if (!owner.getUUID()
                .equals(serverPlayer.getUUID())) {

            send(serverPlayer,
                    "Layla: Only my owner can give me orders.");

            return InteractionResult.SUCCESS;
        }

        // SHIFT + RIGHT-CLICK: INVENTORY
        if (player.isShiftKeyDown()) {
            openLaylaInventory(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        // NORMAL RIGHT-CLICK: ORDERS
        ItemStack held = player.getMainHandItem();

        LaylaMode mode = null;
        LaylaTask task = null;

        // Movement modes
        if (held.is(Items.COMPASS)) {
            mode = LaylaMode.FOLLOW;

        } else if (held.is(Items.CLOCK)) {
            mode = LaylaMode.FOLLOW_AND_WORK;

        } else if (held.is(Items.MAP)) {
            mode = LaylaMode.WORK_INDEPENDENTLY;

        } else if (held.is(Items.WHITE_BED)) {
            mode = LaylaMode.CHILL;
        }

        // Work orders
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

        if (mode != null) {
            LaylaTaskController.setMode(this, mode);

            send(serverPlayer,
                    "Layla: My mode is now "
                            + mode.name());

        } else if (task != null) {
            LaylaTaskController.setTask(this, task);

            send(serverPlayer,
                    "Layla: My task is now "
                            + task.name());

        } else {
            send(serverPlayer,
                    "Layla: Shift + Right-click for inventory.");

            send(serverPlayer,
                    "Layla: Hold a compass, clock, map, "
                            + "bed, log, pickaxe, seeds, fishing rod, "
                            + "crafting table, bricks or torch "
                            + "and right-click to give an order.");

            send(serverPlayer,
                    "Current mode: "
                            + LaylaTaskController.getMode(this)
                            + " | Task: "
                            + LaylaTaskController.getTask(this));
        }

        return InteractionResult.SUCCESS;
    }

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

    private void send(
            ServerPlayer player,
            String message
    ) {
        player.displayClientMessage(
                Component.literal(message),
                false
        );
    }

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

    @Override
    public boolean removeWhenFarAway(
            double distanceSquared
    ) {
        return false;
    }
}
