package com.danikvitek.kvadratutils;

import com.danikvitek.kvadratutils.commands.CommandBlocksCommand;
import com.danikvitek.kvadratutils.commands.ManageEntitiesCommand;
import com.danikvitek.kvadratutils.utils.CustomConfigManager;
import com.danikvitek.kvadratutils.utils.gui.MenuHandler;
import com.danikvitek.kvadratutils.utils.nms.Reflector;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_17;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_8;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Main extends JavaPlugin implements Listener {

    private static MenuHandler menuHandler;
    private Reflector reflector;

    private static File entityManagerFile;
    private static YamlConfiguration modifyEntityManagerFile;
    private static File CBLocationsFile;
    private static YamlConfiguration modifyCBLocationsFile;

    public static File getEntityManagerFile() {
        return entityManagerFile;
    }
    public static YamlConfiguration getModifyEntityManagerFile() {
        return modifyEntityManagerFile;
    }

    public static File getCBLocationsFile() {
        return CBLocationsFile;
    }
    public static YamlConfiguration getModifyCBLocationsFile() {
        return modifyCBLocationsFile;
    }
    public static void setModifyCBLocationsFile(YamlConfiguration modifyCBLocationsFile) {
        Main.modifyCBLocationsFile = modifyCBLocationsFile;
    }

    @Override
    public void onEnable() {
        try {
            initFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

        menuHandler = new MenuHandler();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(menuHandler.getListeners(), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockLogger(), this);

        Objects.requireNonNull(getCommand("command_blocks")).setExecutor(new CommandBlocksCommand());

        ManageEntitiesCommand mec = new ManageEntitiesCommand();
        Bukkit.getPluginManager().registerEvents(mec, this);
        Objects.requireNonNull(getCommand("manage_entities")).setExecutor(mec);

        // Multi-version
        try {
            reflector = new Reflector_1_8();
        } catch (ClassNotFoundException | NoClassDefFoundError e1) {
            try {
                reflector = new Reflector_1_17();
            } catch (ClassNotFoundException | NoClassDefFoundError e2) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Не удалось загрузить плагин для этой версии (" + Bukkit.getVersion() + ")");
            }
        }
    }

    private void initFiles() throws IOException {
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        CBLocationsFile = new File(getDataFolder(), "command_blocks.yml");
        if (!CBLocationsFile.exists())
            CBLocationsFile.createNewFile();
        modifyCBLocationsFile = YamlConfiguration.loadConfiguration(CBLocationsFile);

        CustomConfigManager entityManagerConfig = new CustomConfigManager("entity_manager.yml");
        entityManagerConfig.reloadCustomConfig();
        entityManagerConfig.saveDefaultConfig();
        entityManagerFile = entityManagerConfig.getCustomConfigFile();
        modifyEntityManagerFile = (YamlConfiguration) entityManagerConfig.getCustomConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("kvadratutils.f3n_f3f4"))
            reflector.sendPseudoOPStatus(player);
    }

    @Override
    public void onDisable() {
        menuHandler.closeAll();
    }

    public static MenuHandler getMenuHandler() {
        return menuHandler;
    }
}
