package com.yuushya.modelling.fabric;

import com.yuushya.modelling.YuushyaExpectPlatform;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

public class YuushyaExpectPlatformImpl {
    /**
     * This is our actual method to {@link YuushyaExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return QuiltLoader.getConfigDir();
    }
}
