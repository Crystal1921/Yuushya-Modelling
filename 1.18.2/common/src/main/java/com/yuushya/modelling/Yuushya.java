package com.yuushya.modelling;

import com.yuushya.modelling.registries.YuushyaRegistries;

public class Yuushya {
    public static final String MOD_ID = "yuushya";
    public static void init(){
        YuushyaRegistries.registerAll();
    }
}
