package com.danikvitek.kvadratutils.utils.nms;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Reflector {
    private static final byte OP_STATUS_BYTE = 28; // status byte of OP lever 4
    private static final byte DEOP_STATUS_BYTE = 24; // status byte of OP lever 0

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

    // "net.minecraft.server." + namespace + ".PacketPlayOutPlayerInfo"
    // or
    // "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo"
    protected Class<?> playerInfoPacketClass;

    // "net.minecraft.server." + namespace + ".PacketPlayOutPlayerInfo.EnumPlayerInfoAction"
    // or
    // "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo.EnumPlayerInfoAction"
    protected Class<?> enumPlayerInfoActionClass;

    protected Class<?> respawnPacketClass;

    protected Class<?> dimensionManagerClass;

    protected Class<?> enumDifficultyClass;

    protected Class<?> enumGamemodeClass;

    protected Class<?> resourceKeyClass;

    protected Class<?> worldDataClass;

    protected Class<?> worldClass;

    protected Class<?> worldServerClass;

    protected Class<?> worldTypeClass;

    // "playerConnection"
    // or
    // "b"
    protected String playerConnectionField;

    public Reflector() {
    }

    public void sendPseudoOPStatus(@NotNull Player player) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object packet = entityStatusPacketClass.getConstructor(new Class[] { entityClass, byte.class }).newInstance(entityPlayer, OP_STATUS_BYTE);
            Object playerConnection = entityPlayer.getClass().getDeclaredField(playerConnectionField).get(entityPlayer);
            playerConnectionClass.getDeclaredMethod("sendPacket", packetClass).invoke(playerConnection, packet);
        } catch (Throwable e) {
            throw new RuntimeException("Ошибка во время отправки статуса сущности 28", e);
        }
    }

    public void sendPseudoDeOPStatus(@NotNull Player player) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object packet = entityStatusPacketClass.getConstructor(new Class[] { entityClass, byte.class }).newInstance(entityPlayer, DEOP_STATUS_BYTE);
            Object playerConnection = entityPlayer.getClass().getDeclaredField(playerConnectionField).get(entityPlayer);
            playerConnectionClass.getDeclaredMethod("sendPacket", packetClass).invoke(playerConnection, packet);
        } catch (Throwable e) {
            throw new RuntimeException("Ошибка во время отправки статуса сущности 24", e);
        }
    }

    public void setSkin(@NotNull Player player, @NotNull String title) {
        AtomicReference<String> value = new AtomicReference<>();
        AtomicReference<String> signature = new AtomicReference<>();
        Main.makeExecuteQuery(
                new QueryBuilder().select(Main.skinsTableName)
                        .what("Skin_Value, Skin_Signature")
                        .from()
                        .where("Name = '" + title + "'")
                        .build(),
                new HashMap<>(),
                skinResultSet -> {
                    try {
                        if (skinResultSet.next()) {
                            value.set(skinResultSet.getString(1));
                            signature.set(skinResultSet.getString(2));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return null;
                });

        if (value.get() != null && signature.get() != null) {
            setSkin(player, value.get(), signature.get());
            player.sendMessage(ChatColor.YELLOW + "Вам был присвоен скин " + ChatColor.GOLD + title);
        }
        else
            throw new IllegalArgumentException("Wrong skin title");
    }

    public void setSkin(@NotNull Player player, @NotNull String value, @NotNull String signature) {
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            GameProfile gameProfile = (GameProfile) entityPlayer.getClass().getSuperclass().getDeclaredMethod("getProfile").invoke(entityPlayer);
            PropertyMap propertyMap = gameProfile.getProperties();
            Property property = propertyMap.get("textures").iterator().next();
            propertyMap.remove("textures", property);
            propertyMap.put("textures", new Property("textures", value, signature));

//            PacketPlayOutPlayerInfo
            Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
            Array.set(entityPlayerArray, 0, entityPlayer);
            Object removePlayerPacket = playerInfoPacketClass/*.getConstructors()[MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_17_R1) ? 1 : 0]*/.getConstructor(new Class[] {enumPlayerInfoActionClass,  entityPlayerArray.getClass()}).newInstance(enumPlayerInfoActionClass.getEnumConstants()[4], entityPlayerArray);
            Object addPlayerPacket = playerInfoPacketClass/*.getConstructors()[MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_17_R1) ? 1 : 0]*/.getConstructor(new Class[] {enumPlayerInfoActionClass, entityPlayerArray.getClass()}).newInstance(enumPlayerInfoActionClass.getEnumConstants()[0], entityPlayerArray);

            Object playerConnection = entityPlayer.getClass().getDeclaredField(playerConnectionField).get(entityPlayer);
            playerConnectionClass.getDeclaredMethod("sendPacket", packetClass).invoke(playerConnection, removePlayerPacket);

            final Object respawnPlayerPacket;
            if (MinecraftVersion.VERSION.newerThan(MinecraftVersion.VersionEnum.v1_13_R1)) {
                Object dimensionManagerKey;
                switch (player.getWorld().getEnvironment()) {
                    case NETHER:
                        dimensionManagerKey = dimensionManagerClass.getDeclaredField(MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_17_R1) ? "NETHER" : "g");
//                        dimensionManagerKey = DimensionManagerFieldResolver.resolveAccessor("NETHER", "g").get(null);
                        break;
                    case THE_END:
                        dimensionManagerKey = dimensionManagerClass.getDeclaredField(MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_17_R1) ? "THE_END" : "h");
//                        dimensionManagerKey = DimensionManagerFieldResolver.resolveAccessor("THE_END", "h").get(null);
                        break;
                    case NORMAL:
                    default:
                        dimensionManagerKey = dimensionManagerClass.getDeclaredField(MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_17_R1) ? "OVERWORLD" : "f");
//                        dimensionManagerKey = DimensionManagerFieldResolver.resolveAccessor("OVERWORLD", "f").get(null);
                        break;
                }

                Object difficulty = enumDifficultyClass.getEnumConstants()[player.getWorld().getDifficulty().ordinal()];

                Object gamemode = enumGamemodeClass.getDeclaredMethod("getById", int.class).invoke(null, player.getGameMode().getValue());
                Object type = MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_16_R1) ? ((Object[]) worldTypeClass.getDeclaredField("types").get(null))[0] : null;
                long seedHash = MinecraftVersion.VERSION.olderThan(MinecraftVersion.VersionEnum.v1_15_R1) ? (long) worldDataClass.getDeclaredMethod("c", Long.TYPE).invoke(null, player.getWorld().getSeed()) : -1;

                if (MinecraftVersion.VERSION.newerThan(MinecraftVersion.VersionEnum.v1_16_R2)) {
                    Object nmsWorld = player.getWorld().getClass().getDeclaredMethod("getHandle").invoke(player.getWorld());
                    Object dimensionManager = worldClass.getDeclaredMethod("getDimensionManager").invoke(nmsWorld);
                    Object dimensionKey = worldClass.getDeclaredMethod("getDimensionKey").invoke(nmsWorld);
                    boolean isFlatWorld = (boolean) worldServerClass.getDeclaredMethod("isFlatWorld").invoke(nmsWorld);
                    respawnPlayerPacket = respawnPacketClass
                            .getConstructor(dimensionManagerClass, resourceKeyClass, Long.TYPE, enumGamemodeClass, enumGamemodeClass, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                            .newInstance(dimensionManager, dimensionKey, seedHash, gamemode, gamemode, false/*isDebugWorld*/, isFlatWorld, true/*keepAllPlayerData*/);
                } else if (MinecraftVersion.VERSION.newerThan(MinecraftVersion.VersionEnum.v1_16_R1)) {
                    Object nmsWorld = player.getWorld().getClass().getDeclaredMethod("getHandle").invoke(player.getWorld());
                    Object typeKey = worldClass.getDeclaredMethod("getTypeKey").invoke(nmsWorld);
                    Object dimensionKey = worldClass.getDeclaredMethod("getDimensionKey").invoke(nmsWorld);
                    boolean isFlatWorld = (boolean) worldServerClass.getDeclaredMethod("isFlatWorld").invoke(nmsWorld);
                    respawnPlayerPacket = respawnPacketClass
                            .getConstructor(resourceKeyClass, resourceKeyClass, Long.TYPE, enumGamemodeClass, enumGamemodeClass, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                            .newInstance(typeKey, dimensionKey, seedHash, gamemode, gamemode, false/*isDebugWorld*/, isFlatWorld, true/*keepAllPlayerData*/);
                } else if (MinecraftVersion.VERSION.newerThan(MinecraftVersion.VersionEnum.v1_15_R1)) {// https://wiki.vg/Protocol_History#19w36a - new hashed seed field in respawn packet
                    respawnPlayerPacket = respawnPacketClass
                            .getConstructor(dimensionManagerClass, Long.TYPE, worldTypeClass, enumGamemodeClass)
                            .newInstance(dimensionManagerKey, seedHash, type, gamemode);
                } else if (MinecraftVersion.VERSION.newerThan(MinecraftVersion.VersionEnum.v1_14_R1)) {
                    respawnPlayerPacket = respawnPacketClass
                            .getConstructor(dimensionManagerClass, worldTypeClass, enumGamemodeClass)
                            .newInstance(dimensionManagerKey, type, gamemode);
                } else {
                    respawnPlayerPacket = respawnPacketClass
                            .getConstructor(dimensionManagerClass, enumDifficultyClass, worldTypeClass, enumGamemodeClass)
                            .newInstance(dimensionManagerKey, difficulty, type, gamemode);
                }

            } else {
                int dimension = player.getWorld().getEnvironment().getId();
                Object difficulty = enumDifficultyClass.getDeclaredMethod("getById", int.class).invoke(null, player.getWorld().getDifficulty().getValue());
                Object type = ((Object[]) worldTypeClass.getDeclaredField("types").get(null))[0];
                Object gamemode = enumGamemodeClass.getDeclaredMethod("getById", int.class).invoke(null, player.getGameMode().getValue());

                respawnPlayerPacket = respawnPacketClass
                        .getConstructor(int.class, enumDifficultyClass, worldTypeClass, enumGamemodeClass)
                        .newInstance(dimension, difficulty, type, gamemode);
            }

            final GameMode gameMode = player.getGameMode();
            final boolean allowFlight = player.getAllowFlight();
            final boolean flying = player.isFlying();
            final Location location = player.getLocation();
            final int level = player.getLevel();
            final float xp = player.getExp();
            final double maxHealth = player.getMaxHealth();
            final double health = player.getHealth();
            final boolean isOp = player.isOp();

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        playerConnectionClass.getDeclaredMethod("sendPacket", packetClass).invoke(playerConnection, respawnPlayerPacket);

                        player.setGameMode(gameMode);
                        player.setAllowFlight(allowFlight);
                        player.setFlying(flying);
                        player.teleport(location);
                        player.updateInventory();
                        player.setLevel(level);
                        player.setExp(xp);
                        player.setMaxHealth(maxHealth);
                        player.setHealth(health);
                        player.setOp(isOp);

                        if (player.isOp() || player.hasPermission("kvadratutils.f3n_f3f4"))
                            sendPseudoOPStatus(player);

                        playerConnectionClass.getDeclaredMethod("sendPacket", packetClass).invoke(playerConnection, addPlayerPacket);

                        List<Player> canSee = new ArrayList<>();
                        for (Player player1 : Bukkit.getOnlinePlayers()) {
                            if (player1.canSee(player)) {
                                canSee.add(player1);
                                player1.hidePlayer(Main.getPlugin(Main.class), player);
                            }
                        }
                        for (Player player1 : canSee) {
                            player1.showPlayer(Main.getPlugin(Main.class), player);
                        }
                    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskLater(Main.getPlugin(Main.class), 1L);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public Property getTextureProperty(Player player) {
        Property property = null;
        try {
            Object entityPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            GameProfile gameProfile = (GameProfile) entityPlayer.getClass().getSuperclass().getDeclaredMethod("getProfile").invoke(entityPlayer);
            PropertyMap propertyMap = gameProfile.getProperties();
            property = propertyMap.get("textures").iterator().next();
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return property;
    }
}