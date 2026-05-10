package net.thbtt.itemfilter;

import net.fabricmc.api.ModInitializer;
import net.thbtt.itemfilter.block.ModBlocks;
import net.thbtt.itemfilter.block.entity.ModBlockEntities;
import net.thbtt.itemfilter.screen.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemFilter implements ModInitializer {
    public static final String MOD_ID = "itemfilter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
    }
}