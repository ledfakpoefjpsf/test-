
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

import net.minecraft.world.item.Item;
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
        this.goalSelector.addGoal(
                0, new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                4, new LookAtPlayerGoal(
                        this, Player.class, 8.0F
                )
        );

        this.goalSelector.addGoal(
                5, new RandomLookAroundGoal(this)
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

    // ==========================================
    // RIGHT-CLICK INTERACTION
    // ==========================================

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

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Layla: Only my owner can give me orders."
                    ),
                    false
            );

            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            openInventory(serverPlayer);
        } else {
            openOrders(serverPlayer);
        }

        return InteractionResult.SUCCESS;
    }

    // ==========================================
    // 18-SLOT REAL INVENTORY
    // ==========================================

    private void openInventory(ServerPlayer player) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, menuPlayer) ->
                                new ChestMenu(
                                        MenuType.GENERIC_9x2,
                                        id,
                                        inventory,
                                        LaylaLearningSystem.getInventory(this),
                                        2
                                ),
                        Component.literal("Layla's Inventory")
                )
        );
    }

    // ==========================================
    // ORDERS GUI
    // ==========================================

    private void openOrders(ServerPlayer player) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, menuPlayer) ->
                                new LaylaOrdersMenu(
                                        id, inventory, this
                                ),
                        Component.literal("Layla - Orders")
                )
        );
    }

    // ==========================================
    // CUSTOM CHEST-BASED COMMAND MENU
    // ==========================================

    private static class LaylaOrdersMenu extends ChestMenu {

        private final LaylaEntity layla;
        private final SimpleContainer buttons;

        public LaylaOrdersMenu(
                int id,
                Inventory playerInventory,
                LaylaEntity layla
        ) {
            this(
                    id,
                    playerInventory,
                    layla,
                    new SimpleContainer(27)
            );
        }

        private LaylaOrdersMenu(
                int id,
                Inventory playerInventory,
                LaylaEntity layla,
                SimpleContainer buttons
        ) {
            super(
                    MenuType.GENERIC_9x3,
                    id,
                    playerInventory,
                    buttons,
                    3
            );

            this.layla = layla;
            this.buttons = buttons;

            refreshButtons();
        }

        private void setButton(
                int slot,
                Item item
        ) {
            buttons.setItem(
                    slot,
                    new ItemStack(item)
            );
        }

        private void refreshButtons() {

            buttons.clearContent();

            // Row 1: Movement modes
            setButton(0, Items.COMPASS);        // Follow
            setButton(1, Items.CLOCK);          // Follow + Work
            setButton(2, Items.MAP);            // Independent
            setButton(3, Items.WHITE_BED);      // Chill

            // Row 2: Work orders
            setButton(9, Items.OAK_LOG);        // Woodcutting
            setButton(10, Items.IRON_PICKAXE);  // Mining
            setButton(11, Items.WHEAT_SEEDS);   // Farming
            setButton(12, Items.FISHING_ROD);   // Fishing
            setButton(13, Items.CRAFTING_TABLE);// Crafting
            setButton(14, Items.BRICKS);        // Building
            setButton(15, Items.TORCH);         // Exploring

            // Row 3: Information / Inventory
            setButton(18, Items.CHEST);         // Inventory
            setButton(22, Items.BOOK);          // Status
            setButton(26, Items.BARRIER);       // Close

            broadcastChanges();
        }

        @Override
        public void clicked(
                int slotId,
                int button,
                ClickType clickType,
                Player player
        ) {
            // The first 27 slots are command buttons.
            // Never allow them to be taken or replaced.
            if (slotId >= 0 && slotId < 27) {

                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }

                ServerLevel level = serverPlayer.level();

                ServerPlayer owner =
                        LaylaTaskController.getOwner(layla, level);

                if (owner == null
                        || !owner.getUUID().equals(
                                serverPlayer.getUUID()
                        )) {
                    return;
                }

                if (serverPlayer.distanceToSqr(layla) > 64.0) {
                    serverPlayer.closeContainer();
                    return;
                }

                switch (slotId) {

                    // Movement
                    case 0 -> changeMode(
                            serverPlayer,
                            LaylaMode.FOLLOW
                    );

                    case 1 -> changeMode(
                            serverPlayer,
                            LaylaMode.FOLLOW_AND_WORK
                    );

                    case 2 -> changeMode(
                            serverPlayer,
                            LaylaMode.WORK_INDEPENDENTLY
                    );

                    case 3 -> changeMode(
                            serverPlayer,
                            LaylaMode.CHILL
                    );

                    // Tasks
                    case 9 -> changeTask(
                            serverPlayer,
                            LaylaTask.WOODCUTTING
                    );

                    case 10 -> changeTask(
                            serverPlayer,
                            LaylaTask.MINING
                    );

                    case 11 -> changeTask(
                            serverPlayer,
                            LaylaTask.FARMING
                    );

                    case 12 -> changeTask(
                            serverPlayer,
                            LaylaTask.FISHING
                    );

                    case 13 -> changeTask(
                            serverPlayer,
                            LaylaTask.CRAFTING
                    );

                    case 14 -> changeTask(
                            serverPlayer,
                            LaylaTask.BUILDING
                    );

                    case 15 -> changeTask(
                            serverPlayer,
                            LaylaTask.EXPLORING
                    );

                    // Open Layla's real inventory
                    case 18 -> {
                        serverPlayer.closeContainer();
                        layla.openInventory(serverPlayer);
                    }

                    // Show status
                    case 22 -> {
                        send(
                                serverPlayer,
                                "Mode: " +
                                LaylaTaskController.getMode(layla)
                        );

                        send(
                                serverPlayer,
                                "Task: " +
                                LaylaTaskController.getTask(layla)
                        );
                    }

                    // Close menu
                    case 26 -> serverPlayer.closeContainer();

                    default -> {
                    }
                }

                return;
            }

            // Don't allow shift-clicking or moving items
            // from the player's inventory into button slots.
            if (clickType == ClickType.QUICK_MOVE
                    || clickType == ClickType.QUICK_CRAFT
                    || clickType == ClickType.SWAP
                    || clickType == ClickType.PICKUP_ALL) {
                return;
            }

            super.clicked(
                    slotId,
                    button,
                    clickType,
                    player
            );
        }

        @Override
        public ItemStack quickMoveStack(
                Player player,
                int index
        ) {
            return ItemStack.EMPTY;
        }

        private void changeMode(
                ServerPlayer player,
                LaylaMode mode
        ) {
            LaylaTaskController.setMode(layla, mode);

            send(player, "Layla mode: " + mode.name());

            refreshButtons();
        }

        private void changeTask(
                ServerPlayer player,
                LaylaTask task
        ) {
            LaylaTaskController.setTask(layla, task);

            // Selecting a task automatically enables work.
            LaylaMode mode =
                    LaylaTaskController.getMode(layla);

            if (mode == LaylaMode.CHILL
                    || mode == LaylaMode.FOLLOW) {

                LaylaTaskController.setMode(
                        layla,
                        LaylaMode.FOLLOW_AND_WORK
                );
            }

            send(player, "Layla task: " + task.name());

            refreshButtons();
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

        @Override
        public boolean stillValid(Player player) {
            return layla.isAlive()
                    && player.distanceToSqr(layla) <= 64.0;
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
