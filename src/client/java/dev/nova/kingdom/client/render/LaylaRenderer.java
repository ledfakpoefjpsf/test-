
package dev.nova.kingdom.client.render;

import dev.nova.kingdom.entity.LaylaEntity;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

// @name: LaylaRenderer
// @type: client
// @location: src/client/java/dev/nova/kingdom/client/render/

public class LaylaRenderer extends HumanoidMobRenderer<
        LaylaEntity,
        AvatarRenderState,
        PlayerModel> {

    private static final Identifier LAYLA_SKIN =
            Identifier.fromNamespaceAndPath(
                    "kingdom",
                    "textures/entity/layla/layla_queen.png"
            );

    public LaylaRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new PlayerModel(
                        context.bakeLayer(ModelLayers.PLAYER),
                        false
                ),
                0.5F
        );
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return LAYLA_SKIN;
    }

    @Override
    public void extractRenderState(
            LaylaEntity entity,
            AvatarRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);

        // Show all outer layers of Layla's custom skin.
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
    }
}
