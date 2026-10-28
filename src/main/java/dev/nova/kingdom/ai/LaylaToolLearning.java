
package dev.nova.kingdom.ai;

// @name: LaylaToolLearning
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/LaylaToolLearning.java

import dev.nova.kingdom.entity.LaylaEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LaylaToolLearning {

    public enum Tool {
        AXE,
        PICKAXE,
        HOE,
        SHOVEL,
        SWORD
    }

    private static final Map<UUID, EnumMap<Tool, Boolean>> KNOWLEDGE =
            new HashMap<>();

    private LaylaToolLearning() {
    }

    private static EnumMap<Tool, Boolean> knowledge(
            LaylaEntity layla
    ) {
        return KNOWLEDGE.computeIfAbsent(
                layla.getUUID(),
                id -> new EnumMap<>(Tool.class)
        );
    }

    public static void observeCraft(
            ServerPlayer player,
            ItemStack crafted
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Tool tool = identifyTool(crafted);

        if (tool == null) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(12);

        for (LaylaEntity layla :
                level.getEntitiesOfClass(LaylaEntity.class, area)) {

            knowledge(layla).put(tool, true);
        }
    }

    public static boolean knows(
            LaylaEntity layla,
            Tool tool
    ) {
        return knowledge(layla).getOrDefault(tool, false);
    }

    public static void tick(
            LaylaEntity layla
    ) {
        if (layla.tickCount % 100 != 0) {
            return;
        }

        LaylaTask task = LaylaTaskController.getTask(layla);

        Tool needed = switch (task) {
            case WOODCUTTING -> Tool.AXE;
            case MINING -> Tool.PICKAXE;
            case FARMING -> Tool.HOE;
            case BUILDING -> Tool.SHOVEL;
            default -> null;
        };

        if (needed == null || !knows(layla, needed)) {
            return;
        }

        ItemStack held = layla.getMainHandItem();

        if (identifyTool(held) == needed
                && !isNearlyBroken(held)) {
            return;
        }

        SimpleContainer inventory =
                LaylaLearningSystem.getInventory(layla);

        // First use an existing tool.
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (identifyTool(stack) == needed
                    && !isNearlyBroken(stack)) {

                ItemStack selected = inventory.removeItem(i, 1);

                if (!held.isEmpty()) {
                    ItemStack remainder = inventory.addItem(held.copy());

                    if (!remainder.isEmpty()) {
                        layla.spawnAtLocation(
                                (ServerLevel) layla.level(),
                                remainder
                        );
                    }
                }

                layla.setItemSlot(
                        EquipmentSlot.MAINHAND,
                        selected
                );

                return;
            }
        }

        // Otherwise attempt to craft a wooden tool.
        craftWoodenTool(layla, inventory, needed);
    }

    private static void craftWoodenTool(
            LaylaEntity layla,
            SimpleContainer inventory,
            Tool tool
    ) {
        Item result = switch (tool) {
            case AXE -> Items.WOODEN_AXE;
            case PICKAXE -> Items.WOODEN_PICKAXE;
            case HOE -> Items.WOODEN_HOE;
            case SHOVEL -> Items.WOODEN_SHOVEL;
            case SWORD -> Items.WOODEN_SWORD;
        };

        int planksNeeded = switch (tool) {
            case AXE, PICKAXE -> 3;
            case HOE, SWORD -> 2;
            case SHOVEL -> 1;
        };

        int sticksNeeded = tool == Tool.SWORD ? 1 : 2;

        // Starter implementation: uses oak planks and sticks.
        // More wood types and stone/iron tiers can be added later.
        int planks = count(inventory, Items.OAK_PLANKS);
        int sticks = count(inventory, Items.STICK);

        if (planks < planksNeeded || sticks < sticksNeeded) {
            return;
        }

        remove(inventory, Items.OAK_PLANKS, planksNeeded);
        remove(inventory, Items.STICK, sticksNeeded);

        ItemStack old = layla.getMainHandItem().copy();

        if (!old.isEmpty()) {
            ItemStack remainder = inventory.addItem(old);

            if (!remainder.isEmpty()) {
                layla.spawnAtLocation(
                        (ServerLevel) layla.level(),
                        remainder
                );
            }
        }

        layla.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(result)
        );
    }

    private static int count(
            SimpleContainer inventory,
            Item item
    ) {
        int total = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (stack.is(item)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static void remove(
            SimpleContainer inventory,
            Item item,
            int amount
    ) {
        for (int i = 0;
             i < inventory.getContainerSize() && amount > 0;
             i++) {

            ItemStack stack = inventory.getItem(i);

            if (!stack.is(item)) {
                continue;
            }

            int taken = Math.min(amount, stack.getCount());

            inventory.removeItem(i, taken);
            amount -= taken;
        }
    }

    private static Tool identifyTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.is(Items.WOODEN_AXE)
                || stack.is(Items.STONE_AXE)
                || stack.is(Items.IRON_AXE)
                || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.NETHERITE_AXE)) {
            return Tool.AXE;
        }

        if (stack.is(Items.WOODEN_PICKAXE)
                || stack.is(Items.STONE_PICKAXE)
                || stack.is(Items.IRON_PICKAXE)
                || stack.is(Items.DIAMOND_PICKAXE)
                || stack.is(Items.NETHERITE_PICKAXE)) {
            return Tool.PICKAXE;
        }

        if (stack.is(Items.WOODEN_HOE)
                || stack.is(Items.STONE_HOE)
                || stack.is(Items.IRON_HOE)
                || stack.is(Items.DIAMOND_HOE)
                || stack.is(Items.NETHERITE_HOE)) {
            return Tool.HOE;
        }

        if (stack.is(Items.WOODEN_SHOVEL)
                || stack.is(Items.STONE_SHOVEL)
                || stack.is(Items.IRON_SHOVEL)
                || stack.is(Items.DIAMOND_SHOVEL)
                || stack.is(Items.NETHERITE_SHOVEL)) {
            return Tool.SHOVEL;
        }

        if (stack.is(Items.WOODEN_SWORD)
                || stack.is(Items.STONE_SWORD)
                || stack.is(Items.IRON_SWORD)
                || stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.NETHERITE_SWORD)) {
            return Tool.SWORD;
        }

        return null;
    }

    private static boolean isNearlyBroken(ItemStack stack) {
        return stack.isDamageableItem()
                && stack.getMaxDamage() - stack.getDamageValue() <= 10;
    }
}
