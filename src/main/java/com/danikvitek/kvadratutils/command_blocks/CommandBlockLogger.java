package com.danikvitek.kvadratutils.command_blocks;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.Converter;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandBlockLogger implements Listener { // TODO: log command minecarts
    private static long timestamp = System.currentTimeMillis();
    private static final long period = 2000L;
    private static final long periodGap = 100L;
    public static final BukkitTask newTimeStamp = new BukkitRunnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() >= timestamp)
                timestamp = System.currentTimeMillis() + period;
        }
    }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 0L);

    @EventHandler
    public void onCBPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof CommandBlock) saveCommandBlock(block);
    }

    @EventHandler
    public void onCBMultiPlace(BlockMultiPlaceEvent event) {
        for (BlockState blockState : event.getReplacedBlockStates())
            if (blockState instanceof CommandBlock) saveCommandBlock(blockState.getBlock());
    }

    @EventHandler
    public void onCBBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof CommandBlock) {
            long location = Converter.locationToLong(block.getLocation());
            byte[] worldUUID = Converter.uuidToBytes(block.getWorld().getUID());
            HashMap<Integer, byte[]> values = new HashMap<>();
            values.put(1, worldUUID);
            Integer worldID = Main.makeExecuteQuery(
                    new QueryBuilder().select(Main.worldsTableName).what("ID").from().where("UUID = ?").build(),
                    values,
                    (args, worldsResultSet) -> {
                        try {
                            if (worldsResultSet.next())
                                return worldsResultSet.getInt(1);
                            else return null;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    },
                    null
            );
            if (worldID != null)
                Main.makeExecuteUpdate("DELETE FROM " + Main.cbTableName + " WHERE World_ID = '" + worldID + "' AND Location = '" + location + "';", new HashMap<>());
        }
    }

    @EventHandler
    public void onCBPlaceServerCommand(ServerCommandEvent event) {

    }

    @EventHandler
    public void onCBPlacePlayerCommand(PlayerCommandPreprocessEvent event) {

    }

    @EventHandler
    public void onCBCommand(ServerCommandEvent event) {
        if (event.getSender() instanceof BlockCommandSender) {
            CommandBlock commandBlock = (CommandBlock) ((BlockCommandSender) event.getSender()).getBlock().getState();
            Block block = commandBlock.getBlock();
            if (System.currentTimeMillis() + (period - periodGap) < timestamp) {
                AtomicBoolean isSaved = new AtomicBoolean(false);
                Main.makeExecuteQuery(
                        new QueryBuilder().select(Main.cbTableName).what("*").from().build(), new HashMap<>(),
                        (args, rs) -> {
                            if (rs != null) {
                                int worldID;
                                long location;
                                try {
                                    while (rs.next()) {
                                        worldID = rs.getInt(1);
                                        location = rs.getLong(2);
                                        Block cbBlock = loadCommandBlock(worldID, location);
                                        if (Objects.equals(cbBlock, cbBlock != null ? cbBlock.getState() : null)) {
                                            isSaved.set(true);
                                            break;
                                        }
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                            return false;
                        },
                        null);
                if (!isSaved.get()) saveCommandBlock(block);
            }
            new CommandBlockInstance(block.getLocation(), true);
        }
    }

    /**
     * Сохраняет блок в базу данных
     *
     * @param commandBlock блок, который нужно сохранить
     */
    public static void saveCommandBlock(Block commandBlock) {
        String worldUUID = String.valueOf(commandBlock.getWorld().getUID());
        long location = Converter.locationToLong(commandBlock.getLocation());
        new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<Integer, byte[]> values = new HashMap<>();
                values.put(1, Converter.uuidToBytes(UUID.fromString(worldUUID)));
                Main.makeExecuteQuery(
                        new QueryBuilder().select(Main.worldsTableName) // Select ID of world where world's UUID == value
                                .what("ID")
                                .from()
                                .where("UUID = ?")
                                .build(), values,
                        (args, worldResultSet) -> {
                            if (worldResultSet != null) { // worldResultSet - ID of resulting world
                                try {
                                    worldResultSet.next();
                                    Main.makeExecuteQuery(new QueryBuilder().select(Main.cbTableName) // select CommandBlock where World_ID = value1 and Location = value2
                                                    .what("*")
                                                    .from()
                                                    .where("World_ID = '" + worldResultSet.getInt(1) + "' AND Location = '" + location + "'")
                                                    .build(),
                                            new HashMap<>(),
                                            (args1, cbResultSet) -> {
                                                try {
                                                    if (!cbResultSet.next()) {
                                                        Main.makeExecuteUpdate(new QueryBuilder().insert(Main.cbTableName) // insert cb in table with world_id and location
                                                                .setColumns("World_ID", "Location")
                                                                .setValues("'" + worldResultSet.getInt(1) + "'", "'" + location + "'")
                                                                .onDuplicateKeyUpdate()
                                                                .build(), new HashMap<>());
                                                        return true;
                                                    }
                                                } catch (SQLException e) {
                                                    e.printStackTrace();
                                                }
                                                return false;
                                            },
                                            null
                                    );
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                            return false;
                        }, null);
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
    }

    /**
     * Загружает блок из базы данных
     *
     * @param worldID индекс в базе данных
     * @return CommandBLock extends BlockState если блок найден, иначе null
     * @throws IllegalArgumentException если нет записи с таким идексом
     */
    public static @Nullable Block loadCommandBlock(int worldID, long location) throws IllegalArgumentException {
        return Main.makeExecuteQuery(
                new QueryBuilder().select(Main.cbTableName)
                        .what("World_ID, Location")
                        .from()
                        .where("World_ID = '" + worldID + "' AND Location = '" + location + "'")
                        .build(), new HashMap<>(),
                (args, cbResultSet) -> {
                    if (cbResultSet != null) {
                        try {
                            if (cbResultSet.next()) {
                                return Main.makeExecuteQuery(
                                        new QueryBuilder().select(Main.worldsTableName)
                                                .what("UUID")
                                                .from()
                                                .where("ID = '" + cbResultSet.getInt(1) + "'")
                                                .build(), new HashMap<>(),
                                        (args1, worldResultSet) -> {
                                            if (worldResultSet != null) {
                                                try {
                                                    worldResultSet.next();
                                                    World world = Bukkit.getWorld(Converter.uuidFromBytes(worldResultSet.getBytes(1)));
                                                    long longLocation = cbResultSet.getLong(2);
                                                    Location loc = Converter.locationFromLong(world, longLocation);
                                                    int chunkX = loc.getBlockX() % 16 < 0 ? (loc.getBlockX() % 16) + 16 : loc.getBlockX() % 16,
                                                        chunkY = loc.getBlockY() > 255 ? 255 : Math.max(loc.getBlockY(), 0),
                                                        chunkZ = loc.getBlockZ() % 16 < 0 ? (loc.getBlockZ() % 16) + 16 : loc.getBlockZ() % 16;
                                                    return PaperLib.getChunkAtAsync(loc).get().getBlock(chunkX, chunkY, chunkZ);
                                                } catch (SQLException | ExecutionException | InterruptedException e) {
                                                    e.printStackTrace();
                                                    return null;
                                                }
                                            } else return null;
                                        }, null);
                            } else
                                throw new IllegalArgumentException("Нет командного блока с такими World_ID и Location (" + worldID + ", " + location + ")");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    } else return null;
                }, null);
    }
}