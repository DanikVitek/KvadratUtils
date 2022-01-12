package com.danikvitek.kvadratutils.utils.nms;

public class Reflector_1_18 extends Reflector {
    public Reflector_1_18() throws ClassNotFoundException {
        this.entityStatusPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus");
        this.playerConnectionClass = Class.forName("net.minecraft.server.network.PlayerConnection");
        this.entityClass = Class.forName("net.minecraft.world.entity.Entity");
        this.packetClass = Class.forName("net.minecraft.network.protocol.Packet");
        this.playerConnectionField = "b";
        this.sendPacketMethodName = "a";
        this.getDimensionKeyMethodName = "aa";
        this.getByIdMethodName = "a";
        this.getProfileMethodName = "fp";

        this.playerInfoPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo");
        this.enumPlayerInfoActionClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo").getClasses()[0]; // .EnumPlayerInfoAction
        this.respawnPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutRespawn");
        this.dimensionManagerClass = Class.forName("net.minecraft.world.level.dimension.DimensionManager");
        this.enumDifficultyClass = Class.forName("net.minecraft.world.EnumDifficulty");
        this.enumGamemodeClass = Class.forName("net.minecraft.world.level.EnumGamemode");
        this.resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
        this.worldDataClass = Class.forName("net.minecraft.world.level.storage.WorldData");
        this.worldClass = Class.forName("net.minecraft.world.level.World");
        this.worldServerClass = Class.forName("net.minecraft.server.level.WorldServer");
        this.worldTypeClass = null; // only pre-16
    }
}
