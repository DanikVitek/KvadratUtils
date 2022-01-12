package com.danikvitek.kvadratutils.utils.nms;

import org.bukkit.Bukkit;

public class Reflector_1_8 extends Reflector {
    public Reflector_1_8() throws ClassNotFoundException {
        String namespace = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        this.entityStatusPacketClass = Class.forName("net.minecraft.server." + namespace + ".PacketPlayOutEntityStatus");
        this.playerConnectionClass = Class.forName("net.minecraft.server." + namespace + ".PlayerConnection");
        this.entityClass = Class.forName("net.minecraft.server." + namespace + ".Entity");
        this.packetClass = Class.forName("net.minecraft.server." + namespace + ".Packet");
        this.playerConnectionField = "playerConnection";
        this.sendPacketMethodName = "sendPacket";
        this.getDimensionKeyMethodName = "getDimensionKey";
        this.getByIdMethodName = "getById";
        this.getProfileMethodName = "getProfile";
        this.getDimensionManagerMethodName = "getDimensionManager";

        this.playerInfoPacketClass = Class.forName("net.minecraft.server." + namespace + ".PacketPlayOutPlayerInfo");
        this.enumPlayerInfoActionClass = Class.forName("net.minecraft.server." + namespace + ".PacketPlayOutPlayerInfo").getClasses()[1]; // .EnumPlayerInfoAction
        this.respawnPacketClass = Class.forName("net.minecraft.server." + namespace + ".PacketPlayOutRespawn");
        this.dimensionManagerClass = Class.forName("net.minecraft.server." + namespace + ".DimensionManager");
        this.enumDifficultyClass = Class.forName("net.minecraft.server." + namespace + ".EnumDifficulty");
        this.enumGamemodeClass = Class.forName("net.minecraft.server." + namespace + ".EnumGamemode");
        this.resourceKeyClass = Class.forName("net.minecraft.server." + namespace + ".ResourceKey");
        this.worldDataClass = Class.forName("net.minecraft.server." + namespace + ".WorldData");
        this.worldClass = Class.forName("net.minecraft.server." + namespace + ".World");
        this.worldServerClass = Class.forName("net.minecraft.server." + namespace + ".WorldServer");
        // only pre-16
        if (Integer.parseInt(namespace.split("_")[1]) < 16)
            this.worldTypeClass = Class.forName("net.minecraft.server." + namespace + ".WorldType");
        else
            this.worldTypeClass = null;
    }
}