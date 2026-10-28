
package dev.nova.kingdom.client.render;

// @name: LaylaRenderer
// @type: client
// @location: src/client/java/dev/nova/kingdom/client/render/

import dev.nova.kingdom.entity.LaylaEntity;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;

public class LaylaRenderer extends HumanoidMobRenderer<
        LaylaEntity,
        AvatarRenderState,
        PlayerModel> {

    // Layla's black-and-gold queen skin.
    private static final Identifier LAYLA_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "kingdom",
                    "textures/entity/layla/layla_queen.png"
            );

    // Create a custom player skin record.
    private static final PlayerSkin LAYLA_SKIN =
            new PlayerSkin(
                    new ClientAsset.ResourceTexture(LAYLA_TEXTURE),
                    null,
                    null,
                    PlayerModelType.WIDE,
                    true
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
        AvatarRenderState state = new AvatarRenderState();

        state.skin = LAYLA_SKIN;

        return state;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return LAYLA_TEXTURE;
    }

    @Override
    public void extractRenderState(
            LaylaEntity entity,
            AvatarRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);

        // Force Layla's custom skin each frame.
        state.skin = LAYLA_SKIN;

        // Enable all player skin overlay layers.
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
    }
}
