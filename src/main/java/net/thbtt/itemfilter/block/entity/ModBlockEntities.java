package net.thbtt.itemfilter.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.thbtt.itemfilter.ItemFilter;
import net.thbtt.itemfilter.block.ModBlocks;

public class ModBlockEntities {
    public static final BlockEntityType<ItemFilterBlockEntity> ITEM_FILTER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(ItemFilter.MOD_ID, "item_filter_block_entity"),
                    FabricBlockEntityTypeBuilder.create(
                            ItemFilterBlockEntity::new,
                            ModBlocks.ITEM_FILTER
                    ).build()
            );

    public static void registerBlockEntities() {
        ItemFilter.LOGGER.info("Registering block entities for " + ItemFilter.MOD_ID);
    }
}