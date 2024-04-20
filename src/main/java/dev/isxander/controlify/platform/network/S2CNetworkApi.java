package dev.isxander.controlify.platform.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface S2CNetworkApi extends SidedNetworkApi {
    <T> void sendPacket(ServerPlayer recipient, ResourceLocation channel, T packet);
}