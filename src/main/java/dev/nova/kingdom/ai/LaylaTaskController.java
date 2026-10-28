
package dev.nova.kingdom.ai;

// @name: LaylaTaskController
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/LaylaTaskController.java

import dev.nova.kingdom.entity.LaylaEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LaylaTaskController {

    private static final Map<UUID, LaylaMode> MODES =
            new HashMap<>();

    private static final Map<UUID, LaylaTask> TASKS =
            new HashMap<>();

    private static final Map<UUID, UUID> OWNERS =
            new HashMap<>();

    private LaylaTaskController() {}

    public static LaylaMode getMode(LaylaEntity layla) {
        return MODES.getOrDefault(
                layla.getUUID(),
                LaylaMode.FOLLOW_AND_WORK
        );
    }

    public static LaylaTask getTask(LaylaEntity layla) {
        return TASKS.getOrDefault(
                layla.getUUID(),
                LaylaTask.WOODCUTTING
        );
    }

    public static void setMode(
            LaylaEntity layla,
            LaylaMode mode
    ) {
        MODES.put(layla.getUUID(), mode);

        // Cancel the previous path immediately.
        layla.getNavigation().stop();
        layla.setTarget(null);
    }

    public static void setTask(
            LaylaEntity layla,
            LaylaTask task
    ) {
        TASKS.put(layla.getUUID(), task);

        // Stop traveling toward an old work target.
        layla.getNavigation().stop();
        layla.setTarget(null);
    }

    public static void setOwner(
            LaylaEntity layla,
            ServerPlayer player
    ) {
        OWNERS.put(
                layla.getUUID(),
                player.getUUID()
        );
    }

    public static ServerPlayer getOwner(
            LaylaEntity layla,
            ServerLevel level
    ) {
        UUID ownerId = OWNERS.get(layla.getUUID());

        if (ownerId == null) {
            return null;
        }

        return level.getServer()
                .getPlayerList()
                .getPlayer(ownerId);
    }

    public static boolean canWork(
            LaylaEntity layla,
            ServerLevel level
    ) {
        LaylaMode mode = getMode(layla);

        if (mode != LaylaMode.FOLLOW_AND_WORK
                && mode != LaylaMode.WORK_INDEPENDENTLY) {
            return false;
        }

        ServerPlayer owner = getOwner(layla, level);

        if (owner == null) {
            return false;
        }

        if (owner.level() != level) {
            return false;
        }

        if (mode == LaylaMode.WORK_INDEPENDENTLY) {
            return true;
        }

        return layla.distanceToSqr(owner) <= 100.0;
    }

    public static void tick(
            LaylaEntity layla,
            ServerLevel level
    ) {
        LaylaMode mode = getMode(layla);

        if (mode == LaylaMode.CHILL) {
            layla.getNavigation().stop();
            return;
        }

        if (mode == LaylaMode.WORK_INDEPENDENTLY) {
            // Never force independent Layla to follow.
            return;
        }

        ServerPlayer owner = getOwner(layla, level);

        if (owner == null || owner.level() != level) {
            return;
        }

        if (layla.tickCount % 20 != 0) {
            return;
        }

        double distance = layla.distanceToSqr(owner);

        if (mode == LaylaMode.FOLLOW) {
            if (distance > 9.0) {
                layla.getNavigation().moveTo(owner, 1.1);
            } else {
                layla.getNavigation().stop();
            }

            return;
        }

        // Follow + Work:
        // Follow only when far away, otherwise let work AI
        // control navigation.
        if (mode == LaylaMode.FOLLOW_AND_WORK
                && distance > 100.0) {
            layla.getNavigation().moveTo(owner, 1.1);
        }
    }
}
