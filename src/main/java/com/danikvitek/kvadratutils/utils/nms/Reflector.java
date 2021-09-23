package com.danikvitek.kvadratutils.utils.nms;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

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

    public void sendPseudoOPStatus(@NotNull Player player) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object playerConnection = entityPlayer.getClass().getDeclaredField(this.playerConnectionField).get(entityPlayer);
            Object packet = this.entityStatusPacketClass.getConstructor(new Class[] { this.entityClass, byte.class }).newInstance(entityPlayer, STATUS_BYTE);
            playerConnectionClass.getDeclaredMethod("sendPacket", this.packetClass).invoke(playerConnection, packet);
        } catch (Throwable e) {
            throw new RuntimeException("Ошибка во время отправки статуса сущности 28", e);
        }
    }

    public void setSkin(@NotNull Player player, @NotNull String value, @NotNull String signature) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            GameProfile gameProfile = (GameProfile) entityPlayer.getClass().getDeclaredMethod("getProfile").invoke(entityPlayer);
            PropertyMap propertyMap = gameProfile.getProperties();
            Property property = propertyMap.get("textures").iterator().next();
            propertyMap.remove("textures", property);
            propertyMap.put("textures", new Property("textures", value, signature));
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
