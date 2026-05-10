package net.thbtt.itemfilter.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.thbtt.itemfilter.ItemFilter;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(ItemFilter.MOD_ID, "item_filter"),
                    new ScreenHandlerType<>(ItemFilterScreenHandler::new, FeatureSet.empty())
            );

    public static void registerScreenHandlers() {
        ItemFilter.LOGGER.info("Registering screen handlers for " + ItemFilter.MOD_ID);
    }
}