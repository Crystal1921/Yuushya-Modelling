package com.yuushya.modelling;

import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.registries.YuushyaRegistries;

public class Yuushya {
    public static final String MOD_ID = "yuushya";
    public static final String MOD_ID_USED = "yuushya_modelling";
    public static void init(){
        YuushyaRegistries.registerAll();
        TransformDataNetwork.registerChannel();
    }
}
