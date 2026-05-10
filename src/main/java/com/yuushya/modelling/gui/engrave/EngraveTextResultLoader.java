package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.utils.ClientMethod;
import com.yuushya.modelling.utils.ShareUtils;
import net.minecraft.core.RegistryAccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class EngraveTextResultLoader {
    public static final Map<String, EngraveTextResult> TEXTBLOCK_ITEM_MAP = new HashMap<>();

    public static void load(RegistryAccess registryAccess) {
        if (Files.exists(ClientMethod.TEXT_PATH)) {
            try {
                load(ClientMethod.TEXT_PATH, registryAccess);
            } catch (IOException e) {
                Yuushya.LOG_LOGGER.error(e);
            }
        }
    }

    public static boolean isZip(Path basePath) {
        return basePath.toString().endsWith(".zip");
    }

    private static void loadZip(Path path, RegistryAccess registryAccess) {
        try (FileSystem fileSystem = FileSystems.newFileSystem(path)) {
            load(fileSystem.getPath("."), registryAccess);
        } catch (IOException e) {
            Yuushya.LOG_LOGGER.error(e);
        }
    }

    private static void load(Path path, RegistryAccess registryAccess) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isZip(file)) {
                    loadZip(file, registryAccess);
                } else if (file.getFileName().toString().endsWith(".json")) {
                    String name = path.relativize(file).toString().replaceAll(".json", "");
                    String fileString = Files.readString(file);
                    try {
                        ShareUtils.SharedTextInformation information = ShareUtils.fromText(fileString);
                        TEXTBLOCK_ITEM_MAP.put(name, new EngraveTextResult(name, information, registryAccess));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void saveItem(String string, String name) throws IOException {
        ShareUtils.SharedTextInformation information = ShareUtils.fromText(string);
        TEXTBLOCK_ITEM_MAP.put(name, new EngraveTextResult(name, information));
        Path out = ClientMethod.TEXT_PATH.resolve(name + ".json");
        if (!Files.exists(out)) {
            if (!Files.exists(out.getParent())) Files.createDirectories(out.getParent());
            Files.createFile(out);
        }
        if (Files.exists(out)) {
            Files.writeString(out, string, StandardCharsets.UTF_8);
        }
    }
}
