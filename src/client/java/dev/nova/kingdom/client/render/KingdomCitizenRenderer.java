package dev.nova.kingdom.client.render;

import dev.nova.kingdom.Kingdom;
import dev.nova.kingdom.entity.KingdomVillagerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;

public class KingdomCitizenRenderer extends MobEntityRenderer<KingdomVillagerEntity, VillagerResemblingModel<KingdomVillagerEntity>> {

    private static final Identifier DEFAULT_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/citizen/commoner.png");
    private static final Identifier FARMER_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/citizen/farmer.png");
    private static final Identifier GUARD_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/citizen/guard.png");
    private static final Identifier THIEF_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/citizen/thief.png");
    private static final Identifier MAYOR_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/citizen/mayor.png");

    public KingdomCitizenRenderer(EntityRendererFactory.Context context) {
        super(context, new VillagerResemblingModel<>(context.getPart(EntityModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public Identifier getTexture(KingdomVillagerEntity entity) {
        int role = entity.getRole();
        
        switch (role) {
            case 1: return MAYOR_SKIN;
            case 2: return GUARD_SKIN;
            case 3: return FARMER_SKIN;
            case 7: return THIEF_SKIN;
            default: return DEFAULT_SKIN;
        }
    }
}
