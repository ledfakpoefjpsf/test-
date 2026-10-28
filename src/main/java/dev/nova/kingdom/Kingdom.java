
package dev.nova.kingdom;

import dev.nova.kingdom.entity.LaylaEntity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kingdom implements ModInitializer {

    public static final String MOD_ID = "kingdom";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<EntityType<?>> LAYLA_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    id("layla")
            );

    public static final EntityType<LaylaEntity> LAYLA =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    LAYLA_KEY,
                    EntityType.Builder
                            .of(LaylaEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .build(LAYLA_KEY)
            );

    @Override
    public void onInitialize() {

        LOGGER.info("=================================");
        LOGGER.info("Initializing Kingdom Mod...");
        LOGGER.info("Minecraft Version: 1.21.11");
        LOGGER.info("Mod ID: {}", MOD_ID);
        LOGGER.info("=================================");

        FabricDefaultAttributeRegistry.register(
                LAYLA,
                LaylaEntity.createLaylaAttributes()
        );

        LOGGER.info("Registered Layla entity successfully!");
        LOGGER.info("Kingdom Mod initialization complete!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(
                MOD_ID,
                path
        );
    }
}
