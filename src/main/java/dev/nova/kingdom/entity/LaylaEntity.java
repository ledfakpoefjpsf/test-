
package dev.nova.kingdom.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
            EntityType<? extends LaylaEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setPersistenceRequired();
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

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
