package net.thbtt.itemfilter.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ItemFilterBlockItem extends BlockItem {
    public ItemFilterBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.itemfilter.item_filter.line1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.itemfilter.item_filter.line2").formatted(Formatting.DARK_GRAY));

        super.appendTooltip(stack, context, tooltip, type);
    }
}