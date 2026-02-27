package com.yuushya.modelling.network;

import com.yuushya.modelling.command.ReloadModelCommand;
import com.yuushya.modelling.event.RegistryEvent;
import com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveTextResultLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReloadModelPacket {
    private final ReloadModelCommand.ReloadType reloadType;

    public ReloadModelPacket(ReloadModelCommand.ReloadType reloadType) {
        this.reloadType = reloadType;
    }

    public ReloadModelPacket(FriendlyByteBuf buffer) {
        this.reloadType = buffer.readEnum(ReloadModelCommand.ReloadType.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.reloadType);
    }

    public static void handle(ReloadModelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This packet is received on the client
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                switch (packet.reloadType) {
                    case BLOCKS -> {
                        EngraveBlockResultLoader.SHOWBLOCK_ITEM_MAP.clear();
                        EngraveBlockResultLoader.load(minecraft.level.registryAccess());
                    }
                    case ITEMS -> {
                        EngraveItemResultLoader.ITEMBLOCK_ITEM_MAP.clear();
                        EngraveItemResultLoader.load(minecraft.level.registryAccess());
                    }
                    case TEXTS -> {
                        EngraveTextResultLoader.TEXTBLOCK_ITEM_MAP.clear();
                        EngraveTextResultLoader.load(minecraft.level.registryAccess());
                    }
                    case ALL -> RegistryEvent.load(minecraft.level.registryAccess());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
