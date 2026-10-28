
package dev.nova.kingdom;

// @name: Kingdom
// @type: main mod initializer
// @location: src/main/java/dev/nova/kingdom/Kingdom.java

import dev.nova.kingdom.ai.LaylaLearningSystem;
import dev.nova.kingdom.entity.LaylaEntity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class Kingdom implements ModInitializer {

    public static final String MOD_ID = "kingdom";

    // Layla's entity registry key.
    public static final ResourceKey<EntityType<?>> LAYLA_KEY =
            ResourceKey.create(
                    BuiltInRegistries.ENTITY_TYPE.key(),
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "layla"
                    )
            );

    // Register Layla as a Minecraft entity.
    public static final EntityType<LaylaEntity> LAYLA =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    LAYLA_KEY,
                    EntityType.Builder.of(
                            LaylaEntity::new,
                            MobCategory.CREATURE
                    )
                    .sized(0.6F, 1.8F)
                    .build(LAYLA_KEY)
            );

    @Override
    public void onInitialize() {

        // Register Layla's health and movement attributes.
        FabricDefaultAttributeRegistry.register(
                LAYLA,
                LaylaEntity.createLaylaAttributes()
        );

        // Start Layla's learning system.
        // Registers player observation and learning events.
        LaylaLearningSystem.register();

        System.out.println(
                "[Kingdom] Mod initialized successfully."
        );

        System.out.println(
                "[Kingdom] Layla entity registered."
        );

        System.out.println(
                "[Kingdom] Layla learning system enabled."
        );
    }
}
