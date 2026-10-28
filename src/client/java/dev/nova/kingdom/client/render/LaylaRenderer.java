package dev.nova.kingdom.client.render;

import dev.nova.kingdom.Kingdom;
import dev.nova.kingdom.entity.LaylaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class LaylaRenderer extends MobEntityRenderer<LaylaEntity, PlayerEntityModel<LaylaEntity>> {

    private static final Identifier LAYLA_SKIN = Identifier.of(Kingdom.MOD_ID, "textures/entity/layla/layla_queen.png");

    public LaylaRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(LaylaEntity entity) {
        return LAYLA_SKIN;
    }
}
