package com.danikvitek.kvadratutils.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Map;
import java.util.function.Function;

public class DatabaseManager {
    private final String dbURL, dbUser, dbPassword;

    public static final String worldsTableName = "worlds";
    public static final String cbTableName = "command_blocks";
    public static final String skinsTableName = "skins";
    public static final String playerSkinsTableName = "player_skins";
    public static final String skinRelationTableName = "skin_relations";
    public static final String refCommandsTableName = "reference_commands";

    public DatabaseManager(String host, int port, String dbName, String dbUser, String dbPassword) {
        this.dbURL = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        createTables();
    }

    private void createTables() {
        try (
                Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
                Statement stmt = conn.createStatement()
        ) {
            String sql =
                    "create table if not exists " + worldsTableName + "(" +
                    "   ID int not null auto_increment primary key," +
                    "   UUID binary(16) not null unique," +
                    "   Name varchar(" + Bukkit.getWorlds().stream().map(World::getName).map(String::length).max(Integer::compareTo).orElse(64) + ") not null" +
                    ");" +
                    "";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void makeExecute(String query, @Nullable Map<Integer, Object> values) {
        try (
                Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
                PreparedStatement ps = conn.prepareStatement(query);
        ) {
            if (values != null)
                for (Map.Entry<Integer, Object> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
            ps.execute();
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
            e.printStackTrace();
        }

    }

    public boolean makeExecuteUpdate(String query, @Nullable Map<Integer, Object> values) {
        try (
                Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
                PreparedStatement ps = conn.prepareStatement(query);
        ) {
            if (values != null)
                for (Map.Entry<Integer, Object> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public @Nullable <T> T makeExecuteQuery(String query, @Nullable Map<Integer, Object> values, @Nullable Function<ResultSet, T> function) {
        T result = null;
        try (
                Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
                PreparedStatement ps = conn.prepareStatement(query);
        ) {
            if (values != null)
                for (Map.Entry<Integer, Object> value : values.entrySet())
                    ps.setObject(value.getKey(), value.getValue());
            ResultSet rs = ps.executeQuery();
            if (function != null)
                result = function.apply(rs);
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "QUERY: " + query);
            e.printStackTrace();
        }
        return result;
    }
}
