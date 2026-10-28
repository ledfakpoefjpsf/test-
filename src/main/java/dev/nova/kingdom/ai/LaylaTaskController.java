
package dev.nova.kingdom.ai;

// @name: LaylaTaskController
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/LaylaTaskController.java

import dev.nova.kingdom.entity.LaylaEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

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

    private LaylaTaskController() {
    }

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
    }

    public static void setTask(
            LaylaEntity layla,
            LaylaTask task
    ) {
        TASKS.put(layla.getUUID(), task);
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

        if (owner == null
                || owner.level() != level) {
            return false;
        }

        double maxDistance =
                mode == LaylaMode.FOLLOW_AND_WORK
                        ? 10.0
                        : 60.0;

        return layla.distanceToSqr(owner)
                <= maxDistance * maxDistance;
    }

    public static void tick(
            LaylaEntity layla,
            ServerLevel level
    ) {
        if (layla.tickCount % 20 != 0) {
            return;
        }

        LaylaMode mode = getMode(layla);

        if (mode == LaylaMode.CHILL) {
            layla.getNavigation().stop();
            return;
        }

        ServerPlayer owner = getOwner(layla, level);

        if (owner == null
                || owner.level() != level) {
            return;
        }

        double distanceSquared =
                layla.distanceToSqr(owner);

        double limit =
                mode == LaylaMode.WORK_INDEPENDENTLY
                        ? 60.0
                        : 10.0;

        if (distanceSquared > limit * limit) {
            layla.getNavigation().moveTo(
                    owner,
                    1.2
            );
        }
    }
}
