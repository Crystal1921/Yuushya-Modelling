package com.yuushya.modelling.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class AbstractYuushyaItem extends Item {

    private final Integer tipLines;//注释栏数

    public AbstractYuushyaItem(Properties properties, Integer tipLines) {
        super(properties);
        this.tipLines = tipLines;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> tooltips, @NotNull TooltipFlag flags) {
        for (int i = 1; i <= tipLines; i++)
            tooltips.add(Component.translatable(this.getDescriptionId() + ".line" + i));
    }
}
