package com.danikvitek.kvadratutils;

import com.danikvitek.kvadratutils.commands.*;
import com.danikvitek.kvadratutils.utils.Converter;
import com.danikvitek.kvadratutils.utils.CustomConfigManager;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.gui.MenuHandler;
import com.danikvitek.kvadratutils.utils.nms.Reflector;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_17;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_8;
import com.google.common.collect.Multimap;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.protocol.ResultsetRows;
import io.papermc.lib.PaperLib;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements Listener {

    private static LuckPerms luckPermsAPI;
    private static MenuHandler menuHandler;
    private static Reflector reflector;

    private static File entityManagerFile;
    private static YamlConfiguration modifyEntityManagerFile;
    private static File CBLocationsFile;
    private static YamlConfiguration modifyCBLocationsFile;

    private static DataSource dataSource;
    public static final String worldsTableName = "worlds";
    public static final String cbTableName = "command_blocks";
    public static final String skinsTableName = "skins";

    public static Reflector getReflector() {
        return reflector;
    }

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

    public static LuckPerms getLuckPermsAPI() {
        return luckPermsAPI;
    }

    @Override
    public void onEnable() {
        try {
            initFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PaperLib.suggestPaper(this);

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null)
            luckPermsAPI = provider.getProvider();
        else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "LuckPerms не найден. Отключение...");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        menuHandler = new MenuHandler();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(menuHandler.getListeners(), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockLogger(), this);

        Objects.requireNonNull(getCommand("command_blocks")).setExecutor(new CommandBlocksCommand());

        EntityManagerCommand mec = new EntityManagerCommand();
        Bukkit.getPluginManager().registerEvents(mec, this);
        Objects.requireNonNull(getCommand("entity_manager")).setExecutor(mec);

        getCommand("menus").setExecutor(new MenusCommand());
        getCommand("skin_select").setExecutor(new SkinSelectCommand());
        getCommand("tp_menu").setExecutor(new TPMenuCommand());
        getCommand("manage_permissions").setExecutor(new ManagePermissionsCommand());

        // Database
        MysqlConnectionPoolDataSource mcpDataSource = new MysqlConnectionPoolDataSource();
        mcpDataSource.setServerName(getConfig().getString("database.host"));
        mcpDataSource.setPort(getConfig().getInt("database.port"));
        mcpDataSource.setDatabaseName(getConfig().getString("database.name"));
        mcpDataSource.setUser(getConfig().getString("database.login"));
        mcpDataSource.setPassword(getConfig().getString("database.password"));
        dataSource = mcpDataSource;
        if (!isValidConnection()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Не удалось подключиться к базе данных");
            Bukkit.getPluginManager().disablePlugin(Main.getPlugin(Main.class));
        }

        // Create tables
        try {
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Multi-version
        try {
            reflector = new Reflector_1_8();
        } catch (ClassNotFoundException | NoClassDefFoundError e1) {
            try {
                reflector = new Reflector_1_17();
            } catch (ClassNotFoundException | NoClassDefFoundError e2) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Не удалось загрузить плагин для этой версии (" + Bukkit.getVersion() + ")");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }

        for (Player player: Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("kvadratutils.f3n_f3f4"))
                reflector.sendPseudoOPStatus(player);
        }
    }

    private void initFiles() throws IOException {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

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
        if (!CommandBlockLogger.newTimeStamp.isCancelled())
            CommandBlockLogger.newTimeStamp.cancel();
    }

    public static MenuHandler getMenuHandler() {
        return menuHandler;
    }

    public static void makeExecute(String query, HashMap<Integer, Class<?>> values) {
        Connection connection = getConnection();
        if (connection != null)
            try {
                PreparedStatement ps = connection.prepareStatement(query);
                for (Map.Entry<Integer, Class<?>> value: values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                ps.execute();
                connection.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
            }
    }

    public static @Nullable Integer makeExecuteUpdate(String query, HashMap<Integer, ?> values) {
        Connection connection = getConnection();
        Integer result = null;
        if (connection != null)
            try {
                PreparedStatement ps = connection.prepareStatement(query);
                for (Map.Entry<Integer, ?> value: values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                result = ps.executeUpdate();
                connection.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
            }
        return result;
    }

    public static @Nullable <T> T makeExecuteQuery(String query, HashMap<Integer, ?> values, @Nullable BiFunction<List<?>, ResultSet, T> function, @Nullable List<?> args) {
        Connection conn = getConnection();
        T result = null;
        if (conn != null) {
            try {
                PreparedStatement ps = conn.prepareStatement(query);
                for (Map.Entry<Integer, ?> value: values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                ResultSet rs = ps.executeQuery();
                if (function != null)
                    result = function.apply(args, rs);
                conn.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
            }
        }
        return result;
    }


    private static void initTables() throws SQLException {
        createWorldsTable();
        createCBTable();
        createSkinsTable();
    }

    private static void createWorldsTable() throws SQLException {
        String createTableQuery = new QueryBuilder().createTable(worldsTableName)
                .addAttribute("ID", "INT NOT NULL AUTO_INCREMENT")
                .addAttribute("UUID", "BINARY(16) NOT NULL")
                .addAttribute("Name", "VARCHAR(" + Bukkit.getWorlds().stream().map(World::getName).map(String::length).max(Integer::compareTo).orElse(15) + ") NOT NULL")
                .setPrimaryKeys("ID")
                .build();
        makeExecute(createTableQuery, new HashMap<>());

        makeExecuteQuery(new QueryBuilder().select(worldsTableName).what("*").from().build(), new HashMap<>(),
                (args, rs) -> {
                    if (rs != null) {
                        try {
                            if (!rs.next()) {
                                List<World> worlds = Bukkit.getWorlds();
                                for (World world : worlds) {
                                    HashMap<Integer, byte[]> valuesMap = new HashMap<>();
                                    valuesMap.put(1, Converter.uuidToBytes(world.getUID()));
                                    makeExecuteUpdate(new QueryBuilder().insert(worldsTableName)
                                                    .setColumns("UUID", "Name")
                                                    .setValues("?", "'" + world.getName() + "'")
                                                    .build(),
                                            valuesMap);
                                }
                            }
                            return true;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    return false;
                },
                null);
    }

    private static void createCBTable() throws SQLException {
        String createTableQuery = new QueryBuilder().createTable(cbTableName)
                .addAttribute("ID", "INT NOT NULL AUTO_INCREMENT")
                .addAttribute("World_ID", "INT NOT NULL")
                .addAttribute("Location", "BIGINT NOT NULL")
                .setPrimaryKeys("ID")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }

    private static void createSkinsTable() throws SQLException {
        String createTableQuery = new QueryBuilder().createTable(skinsTableName)
                .addAttribute("ID", "INT NOT NULL AUTO_INCREMENT")
                .addAttribute("Name", "VARCHAR(256) NOT NULL")
                .addAttribute("Skin", "JSON NOT NULL")
                .setPrimaryKeys("ID")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }


    private static boolean isValidConnection() {
        try {
            Connection connection = getConnection();
            if (connection != null)
                return connection.isValid(1000);
            else return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static @Nullable Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
