package net.thbtt.itemfilter;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.thbtt.itemfilter.screen.ItemFilterScreen;
import net.thbtt.itemfilter.screen.ModScreenHandlers;

public class ItemFilterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.ITEM_FILTER_SCREEN_HANDLER, ItemFilterScreen::new);
    }
}