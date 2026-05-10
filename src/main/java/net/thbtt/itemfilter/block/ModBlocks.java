package net.thbtt.itemfilter.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.thbtt.itemfilter.ItemFilter;
import net.thbtt.itemfilter.item.ItemFilterBlockItem;

public class ModBlocks {
    public static final Block ITEM_FILTER = registerBlock(
            "item_filter",
            new ItemFilterBlock(Block.Settings.create()
                    .strength(3.0f, 4.8f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque()
            )
    );

    private static Block registerBlock(String name, Block block) {
        Registry.register(
                Registries.ITEM,
                Identifier.of(ItemFilter.MOD_ID, name),
                new ItemFilterBlockItem(block, new Item.Settings())
        );

        return Registry.register(
                Registries.BLOCK,
                Identifier.of(ItemFilter.MOD_ID, name),
                block
        );
    }

    public static void registerModBlocks() {
        ItemFilter.LOGGER.info("Registering blocks for " + ItemFilter.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(ITEM_FILTER);
        });
    }
}