package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.utils.ShareUtils;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class EngraveItemResultLoader {
    public static final Path PATH = Platform.getModsFolder().resolve("../modellings");

    public static final Map<String, EngraveItemResult> SHOWBLOCK_ITEM_MAP = new HashMap<>();
    public static void load(){
        if(Files.exists(PATH)){
            try {
                load(PATH);
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    public static boolean isZip(Path basePath){
        return basePath.toString().endsWith(".zip");
    }

    private static void loadZip(Path path){
        try(FileSystem fileSystem = FileSystems.newFileSystem(path)){
            load(fileSystem.getPath("."));
        } catch (IOException e) {e.printStackTrace();}
    }
    private static void load(Path path) throws IOException {
        Files.walkFileTree(path,new SimpleFileVisitor<>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
                if(isZip(file)) {
                    loadZip(file);
                }
                else if(file.getFileName().toString().endsWith(".json")){
                    String name = path.relativize(file).toString().replaceAll(".json","");
                    String fileString = Files.readString(file);
                    try{
                        ShareUtils.ShareInformation information = ShareUtils.from(fileString);
                        SHOWBLOCK_ITEM_MAP.put(name,new EngraveItemResult(name, information));
                    }
                    catch (Exception e){e.printStackTrace();}
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    public static void save(String string,String name) throws IOException{
        ShareUtils.ShareInformation information = ShareUtils.from(string);
        SHOWBLOCK_ITEM_MAP.put(name,new EngraveItemResult(name, information));
        TransformDataListNetwork.updateSendingCache(name);
        Path out = PATH.resolve("./"+name+".json");
        if(!Files.exists(out)){
            if(!Files.exists(out.getParent())) Files.createDirectories(out.getParent());
            Files.createFile(out);
        }
        if(Files.exists(out)){
            Files.writeString(out,string, StandardCharsets.UTF_8);
        }
    }
}