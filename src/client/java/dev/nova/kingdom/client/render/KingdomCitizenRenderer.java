
package dev.nova.kingdom.client.render;

import dev.nova.kingdom.entity.KingdomVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

// @name: KingdomCitizenRenderer
// @type: Java client renderer
// @location: src/client/java/dev/nova/kingdom/client/render/
//
// Temporary renderer to keep the mod compiling.
// We will add visible citizen models later.

public class KingdomCitizenRenderer
        extends EntityRenderer<KingdomVillagerEntity, EntityRenderState> {

    public KingdomCitizenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
