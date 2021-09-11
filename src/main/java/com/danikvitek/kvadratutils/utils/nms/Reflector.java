package com.danikvitek.kvadratutils.utils.nms;

import org.bukkit.entity.Player;

public class Reflector {
    private static final byte STATUS_BYTE = 28; // status byte of OP lever 4

    // "net.minecraft.server." + namespace + ".PacketPlayOutEntityStatus"
    // or
    // "net.minecraft.network.protocol.game.PacketPlayOutEntityStatus"
    protected Class<?> entityStatusPacketClass;

    // "net.minecraft.server." + namespace + ".PlayerConnection"
    // or
    // "net.minecraft.server.network.PlayerConnection"
    protected Class<?> playerConnectionClass;

    // "net.minecraft.server." + namespace + ".Entity"
    // or
    // "net.minecraft.world.entity.Entity"
    protected Class<?> entityClass;

    // "net.minecraft.server." + namespace + ".Packet"
    // or
    // "net.minecraft.network.protocol.Packet"
    protected Class<?> packetClass;

    // "playerConnection"
    // or
    // "b"
    protected String playerConnectionField;

    public Reflector() {}

    public void sendPseudoOPStatus(Player player) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object playerConnection = entityPlayer.getClass().getDeclaredField(this.playerConnectionField).get(entityPlayer);
            Object packet = this.entityStatusPacketClass.getConstructor(new Class[] { this.entityClass, byte.class }).newInstance(entityPlayer, STATUS_BYTE);
            playerConnectionClass.getDeclaredMethod("sendPacket", this.packetClass).invoke(playerConnection, packet);
        } catch (Throwable e) {
            throw new RuntimeException("Ошибка во время отправки статуса сущности 28", e);
        }
    }
}
