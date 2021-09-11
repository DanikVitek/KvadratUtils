package com.danikvitek.kvadratutils.utils.nms;

public class Reflector_1_17 extends Reflector {
    public Reflector_1_17() throws ClassNotFoundException {
        this.entityStatusPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus");
        this.playerConnectionClass = Class.forName("net.minecraft.server.network.PlayerConnection");
        this.entityClass = Class.forName("net.minecraft.world.entity.Entity");
        this.packetClass = Class.forName("net.minecraft.network.protocol.Packet");
        this.playerConnectionField = "b";
    }
}
