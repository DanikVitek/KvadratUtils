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
    }
}
