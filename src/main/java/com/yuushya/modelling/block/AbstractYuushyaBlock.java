package com.yuushya.modelling.block;

import net.minecraft.world.level.block.Block;

public class AbstractYuushyaBlock extends Block {

    private final Integer tipLines;//注释栏数

    public AbstractYuushyaBlock(Properties properties, Integer tipLines) {
        super(properties);
        this.tipLines = tipLines;
    }
}
