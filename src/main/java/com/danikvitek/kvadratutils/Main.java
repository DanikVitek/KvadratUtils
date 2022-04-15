package com.danikvitek.kvadratutils;

import com.danikvitek.kvadratutils.command_blocks.CommandBlockLogger;
import com.danikvitek.kvadratutils.commands.*;
import com.danikvitek.kvadratutils.utils.Converter;
import com.danikvitek.kvadratutils.utils.CustomConfigManager;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.annotations.ConfigVar;
import com.danikvitek.kvadratutils.utils.gui.MenuHandler;
import com.danikvitek.kvadratutils.utils.nms.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import io.papermc.lib.PaperLib;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.model.user.User;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {
    private static final Logger logger = Bukkit.getLogger();

    private static Main instance;

    public static void log(String message) {
        logger.info(String.format("[%s] %s", instance.getName(), message));
    }

    public static @NotNull Main getInstance() {
        return instance;
    }

    @ConfigVar("MineSkin-API.key")
    private static String API_KEY;
    @ConfigVar("MineSkin-API.user-agent")
    private static String API_UserAgent;

    public static String getAPIKey() {
        return API_KEY;
    }

    public static String getAPIUserAgent() {
        return API_UserAgent;
    }

    private static LuckPerms luckPermsAPI;
    private static MenuHandler menuHandler;
    private static Reflector reflector;

    private static File entityManagerFile;
    private static YamlConfiguration modifyEntityManagerFile;

    private static DataSource dataSource;
    public static final String WORLDS_TABLE_NAME = "worlds";
    public static final String CB_TABLE_NAME = "command_blocks";
    public static final String SKINS_TABLE_NAME = "skins";
    public static final String PLAYER_SKINS_TABLE_NAME = "player_skins";
    public static final String SKIN_RELATION_TABLE_NAME = "skin_relations";
    public static final String REF_COMMANDS_TABLE_NAME = "reference_commands";

    public static Reflector getReflector() {
        return reflector;
    }

    public static File getEntityManagerFile() {
        return entityManagerFile;
    }

    public static YamlConfiguration getModifyEntityManagerFile() {
        return modifyEntityManagerFile;
    }

    public static LuckPerms getLuckPermsAPI() {
        return luckPermsAPI;
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            initFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ConfigVar.Manager.register(this.getClass());
        ConfigVar.Manager.update();

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

        EntityManagerCommand entityManagerCommand = new EntityManagerCommand();
        Bukkit.getPluginManager().registerEvents(entityManagerCommand, this);
        Objects.requireNonNull(getCommand("entity_manager")).setExecutor(entityManagerCommand);

        TPMenuCommand tpMenuCommand = new TPMenuCommand();
        Bukkit.getPluginManager().registerEvents(tpMenuCommand, this);
        Objects.requireNonNull(getCommand("tp_menu")).setExecutor(tpMenuCommand);

        SkinSelectCommand skinSelectCommand = new SkinSelectCommand();
        Objects.requireNonNull(getCommand("skin_select")).setExecutor(skinSelectCommand);

        SkinCommand skinCommand = new SkinCommand();
        Bukkit.getPluginManager().registerEvents(skinCommand, this);
        Objects.requireNonNull(getCommand("skin")).setExecutor(skinCommand);

        Objects.requireNonNull(getCommand("menus")).setExecutor(new MenusCommand());
        Objects.requireNonNull(getCommand("manage_permissions")).setExecutor(new ManagePermissionsCommand());
        Objects.requireNonNull(getCommand("gamerules")).setExecutor(new GameRulesCommand());
        Objects.requireNonNull(getCommand("refcommand")).setExecutor(new RefCommandCommand());
        Objects.requireNonNull(getCommand("ref")).setExecutor(new RefCommandExecutor());

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
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Create tables
        initTables();

        // Multi-version
        switch (MinecraftVersion.VERSION) {
            case v1_8_R1:
            case v1_8_R2:
            case v1_8_R3:
            case v1_8_R4:
            case v1_9_R1:
            case v1_9_R2:
            case v1_10_R1:
            case v1_11_R1:
            case v1_12_R1:
            case v1_13_R1:
            case v1_13_R2:
            case v1_14_R1:
            case v1_15_R1:
            case v1_16_R1:
            case v1_16_R2:
            case v1_16_R3:
                try {
                    reflector = new Reflector_1_8();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case v1_17_R1:
                try {
                    reflector = new Reflector_1_17();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case v1_18_R1:
                try {
                    reflector = new Reflector_1_18();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case v1_18_R2:
                try {
                    reflector = new Reflector_1_18_2();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("kvadratutils.f3n_f3f4"))
                reflector.sendPseudoOPStatus(player);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!Objects.requireNonNull(luckPermsAPI.getUserManager().getUser(player.getUniqueId()))
                            .getNodes(NodeType.PERMISSION).stream()
                            .map(PermissionNode::getPermission)
                            .collect(Collectors.toList())
                            .contains("kvadratutils.not_teleport")
                    ) luckPermsAPI.getUserManager().modifyUser(
                            player.getUniqueId(),
                            u -> u.data().add(PermissionNode
                                    .builder("kvadratutils.not_teleport")
                                    .value(false)
                                    .build())
                    );
                    if (!Objects.requireNonNull(luckPermsAPI.getUserManager().getUser(player.getUniqueId()))
                            .getNodes(NodeType.PERMISSION).stream()
                            .map(PermissionNode::getPermission)
                            .collect(Collectors.toList())
                            .contains("kvadratutils.not_teleport_to")
                    ) luckPermsAPI.getUserManager().modifyUser(
                            player.getUniqueId(),
                            u -> u.data().add(PermissionNode
                                    .builder("kvadratutils.not_teleport_to")
                                    .value(false)
                                    .build())
                    );
                }
            }
        }.runTaskAsynchronously(this);

        Iterator<? extends Player> playerQueue = Bukkit.getOnlinePlayers().iterator();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerQueue.hasNext()) {
                    Player player = playerQueue.next();
                    try {
                        org.jsoup.Connection.Response response = Jsoup
                                .connect("https://api.mineskin.org/generate/user?name=" + player.getName() + "&uuid=" + player.getUniqueId())
                                .userAgent(API_UserAgent)
                                .header("Authorization", API_KEY)
                                .method(org.jsoup.Connection.Method.POST)
                                .ignoreContentType(true)
                                .ignoreHttpErrors(true)
                                .timeout(40000)
                                .execute();
                        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

                        if (response.statusCode() == 200) {
                            String value = jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("value").getAsString(),
                                    signature = jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("signature").getAsString();
                            Map<Integer, byte[]> values = new HashMap<>();
                            values.put(1, Converter.uuidToBytes(player.getUniqueId()));
                            values.put(2, Converter.uuidToBytes(player.getUniqueId()));
                            Main.makeExecuteUpdate(
                                    new QueryBuilder().insert(Main.PLAYER_SKINS_TABLE_NAME)
                                            .setColumns("Player", "Skin_Value", "Skin_Signature")
                                            .setValues("?", "'" + value + "'", "'" + signature + "'")
                                            .onDuplicateKeyUpdate()
                                            .build(),
                                    values);
                        } else {
                            getLogger().log(Level.ALL, response.url().toString());
                            getLogger().log(Level.ALL, jsonResponse.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else
                    cancel();
            }
        }.runTaskTimerAsynchronously(this, 0L, 140L);

        luckPermsAPI.getEventBus().subscribe(
                this,
                NodeAddEvent.class,
                event -> {
                    if (event.getNode() instanceof PermissionNode) {
                        PermissionNode permissionNode = (PermissionNode) event.getNode();
                        if (permissionNode.getPermission().equals("kvadratutils.f3n_f3f4") && event.isUser()) {
                            if (permissionNode.getValue())
                                reflector.sendPseudoOPStatus(Objects.requireNonNull(Bukkit.getPlayer(((User) event.getTarget()).getUniqueId())));
                            else
                                reflector.sendPseudoDeOPStatus(Objects.requireNonNull(Bukkit.getPlayer(((User) event.getTarget()).getUniqueId())));
                        }
                    }
                });
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Objects.requireNonNull(luckPermsAPI.getUserManager().getUser(player.getUniqueId()))
                        .getNodes(NodeType.PERMISSION).stream()
                        .map(PermissionNode::getPermission)
                        .collect(Collectors.toList())
                        .contains("kvadratutils.not_teleport")
                ) luckPermsAPI.getUserManager().modifyUser(
                        player.getUniqueId(),
                        u -> u.data().add(PermissionNode
                                .builder("kvadratutils.not_teleport")
                                .value(false)
                                .build())
                );
                if (!Objects.requireNonNull(luckPermsAPI.getUserManager().getUser(player.getUniqueId()))
                        .getNodes(NodeType.PERMISSION).stream()
                        .map(PermissionNode::getPermission)
                        .collect(Collectors.toList())
                        .contains("kvadratutils.not_teleport_to")
                ) luckPermsAPI.getUserManager().modifyUser(
                        player.getUniqueId(),
                        u -> u.data().add(PermissionNode
                                .builder("kvadratutils.not_teleport_to")
                                .value(false)
                                .build())
                );
                Property playerSkin = reflector.getTextureProperty(player);
                HashMap<Integer, byte[]> values = new HashMap<>();
                values.put(1, Converter.uuidToBytes(player.getUniqueId()));
                values.put(2, Converter.uuidToBytes(player.getUniqueId()));
                makeExecuteUpdate(new QueryBuilder().insert(PLAYER_SKINS_TABLE_NAME)
                                .setColumns("Player", "Skin_Value", "Skin_Signature")
                                .setValues("?", "'" + playerSkin.getValue() + "'", "'" + playerSkin.getSignature() + "'")
                                .onDuplicateKeyUpdate()
                                .build(),
                        values);
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onDisable() {
        if (menuHandler != null)
            menuHandler.closeAll();

        if (!CommandBlockLogger.newTimeStamp.isCancelled())
            CommandBlockLogger.newTimeStamp.cancel();

        if (!EntityManagerCommand.removeXPOrbsInChunksTask.isCancelled())
            EntityManagerCommand.removeXPOrbsInChunksTask.cancel();

        if (!TPMenuCommand.collisionCooldownTask.isCancelled())
            TPMenuCommand.collisionCooldownTask.cancel();
    }

    public static MenuHandler getMenuHandler() {
        return menuHandler;
    }

    public static void makeExecute(String query, Map<Integer, Class<?>> values) {
        Connection connection = getConnection();
        if (connection != null)
            try {
                PreparedStatement ps = connection.prepareStatement(query);
                for (Map.Entry<Integer, Class<?>> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                ps.execute();
                connection.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
            }
    }

    public static boolean makeExecuteUpdate(String query, Map<Integer, ?> values) {
        Connection connection = getConnection();
        if (connection != null)
            try {
                PreparedStatement ps = connection.prepareStatement(query);
                for (Map.Entry<Integer, ?> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                ps.executeUpdate();
                connection.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
                return false;
            }
        return true;
    }

    public static @Nullable <T> T makeExecuteQuery(String query, Map<Integer, ?> values, @Nullable Function<ResultSet, T> function) {
        Connection conn = getConnection();
        T result = null;
        if (conn != null) {
            try {
                PreparedStatement ps = conn.prepareStatement(query);
                for (Map.Entry<Integer, ?> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
                ResultSet rs = ps.executeQuery();
                if (function != null)
                    result = function.apply(rs);
                conn.close();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
                e.printStackTrace();
            }
        }
        return result;
    }


    private static void initTables() {
        createWorldsTable();
        createCBTable();
        createSkinsTable();
        createPlayerSkinsTable();
        createSkinsRelationTable();
        createRefCommandsTable();
    }

    private static void createWorldsTable() {
        String createTableQuery = new QueryBuilder().createTable(WORLDS_TABLE_NAME)
                .addAttribute("ID", "INT NOT NULL AUTO_INCREMENT")
                .addAttribute("UUID", "BINARY(16) NOT NULL")
                .addAttribute("Name", "VARCHAR(" + Bukkit.getWorlds().stream().map(World::getName).map(String::length).max(Integer::compareTo).orElse(64) + ") NOT NULL")
                .setPrimaryKeys("ID")
                .build();
        makeExecute(createTableQuery, new HashMap<>());

        makeExecuteQuery(new QueryBuilder().select(WORLDS_TABLE_NAME).what("*").from().build(), new HashMap<>(),
                rs -> {
                    if (rs != null) {
                        try {
                            if (!rs.next()) {
                                List<World> worlds = Bukkit.getWorlds();
                                for (World world : worlds) {
                                    HashMap<Integer, byte[]> valuesMap = new HashMap<>();
                                    valuesMap.put(1, Converter.uuidToBytes(world.getUID()));
                                    makeExecuteUpdate(new QueryBuilder().insert(WORLDS_TABLE_NAME)
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
                });
    }

    private static void createCBTable() {
        String createTableQuery = new QueryBuilder().createTable(CB_TABLE_NAME)
//                .addAttribute("ID", "INT NOT NULL AUTO_INCREMENT")
                .addAttribute("World_ID", "INT NOT NULL")
                .addAttribute("Location", "BIGINT NOT NULL")
//                .setPrimaryKeys("ID")
                .setPrimaryKeys("World_ID", "Location")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }

    private static void createSkinsTable() {
        String createTableQuery = new QueryBuilder().createTable(SKINS_TABLE_NAME)
                .addAttribute("Name", "VARCHAR(256) NOT NULL")
                .addAttribute("Skin_Value", "BLOB NOT NULL")
                .addAttribute("Skin_Signature", "BLOB NOT NULL")
                .setPrimaryKeys("Name")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }

    private static void createSkinsRelationTable() {
        String createTableQuery = new QueryBuilder().createTable(SKIN_RELATION_TABLE_NAME)
                .addAttribute("Player", "BINARY(16) NOT NULL")
                .addAttribute("Skin_Name", "VARCHAR(256)")
                .setPrimaryKeys("Player")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }

    private static void createPlayerSkinsTable() {
        String createTableQuery = new QueryBuilder().createTable(PLAYER_SKINS_TABLE_NAME)
                .addAttribute("Player", "BINARY(16) NOT NULL")
                .addAttribute("Skin_Value", "BLOB NOT NULL")
                .addAttribute("Skin_Signature", "BLOB NOT NULL")
                .setPrimaryKeys("Player")
                .build();
        makeExecute(createTableQuery, new HashMap<>());
    }

    private static void createRefCommandsTable() {
        String createTableQuery = new QueryBuilder().createTable(REF_COMMANDS_TABLE_NAME)
                .addAttribute("Alias", "VARCHAR(50) NOT NULL")
                .addAttribute("Command", "TEXT NOT NULL")
                .setPrimaryKeys("Alias")
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