package dev.nova.kingdom.client.render;

import dev.nova.kingdom.entity.LaylaEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Temporary invisible renderer to confirm Layla can spawn without a crash.
 * Replace with a textured humanoid renderer after the spawn test succeeds.
 */
public class LaylaRenderer extends EntityRenderer<LaylaEntity, EntityRenderState> {
    public LaylaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
