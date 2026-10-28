
package dev.nova.kingdom.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

public abstract class KingdomVillagerEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> SKILL_LEVEL =
            SynchedEntityData.defineId(
                    KingdomVillagerEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> NPC_ROLE =
            SynchedEntityData.defineId(
                    KingdomVillagerEntity.class,
                    EntityDataSerializers.INT
            );

    protected int skillXp = 0;

    protected KingdomVillagerEntity(
            EntityType<? extends PathfinderMob> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);

        builder.define(SKILL_LEVEL, 1);
        builder.define(NPC_ROLE, 0);
    }

    public static AttributeSupplier.Builder createKingdomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    public void addSkillXp(int amount) {
        if (amount <= 0) {
            return;
        }

        this.skillXp += amount;

        int currentLevel = getSkillLevel();

        while (this.skillXp >= currentLevel * 100) {
            this.skillXp -= currentLevel * 100;
            currentLevel++;

            this.entityData.set(SKILL_LEVEL, currentLevel);
        }

        updateMovementSpeed();
    }

    private void updateMovementSpeed() {
        int level = getSkillLevel();

        double newSpeed = 0.3 + ((level - 1) * 0.01);

        var movementAttribute =
                this.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementAttribute != null) {
            movementAttribute.setBaseValue(newSpeed);
        }
    }

    public int getSkillLevel() {
        return this.entityData.get(SKILL_LEVEL);
    }

    public int getSkillXp() {
        return this.skillXp;
    }

    public void setRole(int roleId) {
        this.entityData.set(NPC_ROLE, roleId);
    }

    public int getRole() {
        return this.entityData.get(NPC_ROLE);
    }
}
