package dev.nova.kingdom.client;

import dev.nova.kingdom.Kingdom;
import dev.nova.kingdom.client.render.LaylaRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class KingdomClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register Layla's renderer
        EntityRendererRegistry.register(Kingdom.LAYLA, LaylaRenderer::new);
        
        // If you register a citizen entity type later, register its renderer here too:
        // EntityRendererRegistry.register(Kingdom.CITIZEN, KingdomCitizenRenderer::new);
    }
}
