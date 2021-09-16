package com.danikvitek.kvadratutils;

import com.danikvitek.kvadratutils.commands.*;
import com.danikvitek.kvadratutils.utils.CustomConfigManager;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.gui.MenuHandler;
import com.danikvitek.kvadratutils.utils.nms.Reflector;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_17;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_8;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class Main extends JavaPlugin implements Listener {

    private static LuckPerms luckPermsAPI;
    private static MenuHandler menuHandler;
    private static Reflector reflector;

    private static File entityManagerFile;
    private static YamlConfiguration modifyEntityManagerFile;
    private static File CBLocationsFile;
    private static YamlConfiguration modifyCBLocationsFile;

    private static DataSource dataSource;
    public static final String cbTableName = "command_blocks";


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
            Bukkit.getPluginManager().disablePlugin(this);
        }
        // Create tables
        try {
            createCBTable();
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
        try {
            getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static MenuHandler getMenuHandler() {
        return menuHandler;
    }

    public static PreparedStatement makeQuery(String query) throws SQLException {
        try {
            return getConnection().prepareStatement(query);
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
            throw e;
        }
    }

    private static void createCBTable() throws SQLException {
        ResultSet rs = makeQuery("SHOW TABLES").executeQuery();
        String createTableQuery = new QueryBuilder().createTable(cbTableName)
                .addAttribute("ID", "INT")
                .addAttribute("Location", "BIGINT")
                .addAttribute("World", "BINARY(16)")
                .setPrimaryKeys("ID")
                .build();
        if (!rs.next())
            makeQuery(createTableQuery).execute();
        else {
            boolean createTable = true;
            do
                if (rs.getString(1).equals(cbTableName)) {
                    createTable = false;
                    break;
                }
            while (!rs.getString(1).equals(cbTableName) && rs.next());
            if (createTable)
                makeQuery(createTableQuery).execute();
        }
    }

    private static boolean isValidConnection() {
        try {
            Connection connection = getConnection();
            return connection.isValid(1000);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
