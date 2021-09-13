package com.danikvitek.kvadratutils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.io.IOException;
import java.util.UUID;

public class CommandBlockLogger implements Listener { // TODO: log command minecarts
    @EventHandler
    public void onCBPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof CommandBlock) {
            Material type = block.getType();
            boolean conditional = ((org.bukkit.block.data.type.CommandBlock) block.getBlockData()).isConditional();
            BlockFace facing = ((org.bukkit.block.data.type.CommandBlock) block.getBlockData()).getFacing();
            String command = ((CommandBlock) block.getState()).getCommand();
//            TileEntityCommand
        }
    }

    @EventHandler
    public void onCBMultiPlace(BlockMultiPlaceEvent event) {

    }

    @EventHandler
    public void onCBBreak(BlockBreakEvent event) {

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
            boolean isSaved = false;
            for (String key: Main.getModifyCBLocationsFile().getKeys(false))
                if (((BlockCommandSender) event.getSender()).getBlock().getState().equals(loadCommandBlock(key))) {
                    isSaved = true;
                    break;
                }
            if (!isSaved)
                saveCommandBlock(((BlockCommandSender) event.getSender()).getBlock());
            new CommandBlockInstance(((BlockCommandSender) event.getSender()).getBlock().getLocation());
        }
    }

    /** Сохраняет блок в файл
     * @param commandBlock блок, который нужно сохранить
     * @return true если успешно сохронён, иначе false
     */
    public static boolean saveCommandBlock(Block commandBlock) {
        String key = String.valueOf(Main.getModifyCBLocationsFile().getKeys(false).stream().map(Long::parseUnsignedLong).max(Long::compareUnsigned).orElse(0L) + 1L);
        String uuid = String.valueOf(commandBlock.getWorld().getUID());
        long location = locationToLong(commandBlock.getLocation());
        Main.getModifyCBLocationsFile().set(key + ".world", uuid);
        Main.getModifyCBLocationsFile().set(key + ".location", location);
        try {
            Main.getModifyCBLocationsFile().save(Main.getCBLocationsFile());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** загружает блок из файла конфигурации
     * @param key ключ ConfigurationSection
     * @return CommandBLock extends BlockState если блок найден, иначе null
     * @throws IllegalArgumentException если нет ConfigurationSection с таким ключём
     */
    public static CommandBlock loadCommandBlock(String key) throws IllegalArgumentException {
        ConfigurationSection blockSection = Main.getModifyCBLocationsFile().getConfigurationSection(key);
        if (blockSection != null) {
            Location location = locationFromLong(
                    blockSection.getString("world"),
                    blockSection.getLong("location")
            );
            if (location.getBlock().getState() instanceof CommandBlock)
                return (CommandBlock) location.getBlock().getState();
            else {
                Main.getModifyCBLocationsFile().set(key, null);
                try {
                    Main.getModifyCBLocationsFile().save(Main.getCBLocationsFile());
                } catch (IOException e) {
                    if (!Main.getCBLocationsFile().exists()) {
                        try {
                            Main.getCBLocationsFile().createNewFile();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    Main.setModifyCBLocationsFile(YamlConfiguration.loadConfiguration(Main.getCBLocationsFile()));
                }
                return null;
            }
        }
        else
            throw new IllegalArgumentException("Нет секции с таким ключём (" + key + ")");
    }

    public static Location locationFromLong(final String uuid, final long l) {
        return new Location(
                Bukkit.getWorld(UUID.fromString(uuid)),
                (int)(l >> 38),
                (int)(l << 26 >> 52),
                (int)(l << 38 >> 38)
        );
    }

    public static long locationToLong(final Location l) {
        return ((long) l.getX() & 67108863) << 38 | ((long) l.getY() & 4095) << 26 | ((long) l.getZ() & 67108863);
    }
}
