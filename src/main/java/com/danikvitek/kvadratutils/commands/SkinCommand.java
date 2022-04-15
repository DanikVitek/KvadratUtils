package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.Converter;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.RandomContainer;
import com.danikvitek.kvadratutils.utils.skin.SkinOptions;
import com.danikvitek.kvadratutils.utils.skin.Variant;
import com.danikvitek.kvadratutils.utils.skin.Visibility;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

public class SkinCommand implements TabExecutor, Listener {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //        0     1        2     |   0      1        2           3           0      1            0      1          0        1         2
        // skin <set <title> [nickname]> | <save <title> <image_url> [is_slim]> | <reset [nickname]> | <delete <title>> | <player <nickname> [title]> | <reload [nickname]>
        if (args.length == 1) {
            if (args[0].equals("reset")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission("kvadratutils.command.skin.reset")) {
                        resetSkinRelation(player);
                        player.sendMessage(ChatColor.YELLOW + "Ваш скин был сброшен");
                    } else
                        player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                } else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Сбросить себе скин может только игрок");
            }
            else if (args[0].equals("reload")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission("kvadratutils.command.skin.reload")) {
                        reloadSkin(player);
                        player.sendMessage(ChatColor.YELLOW + "Ваш скин был перезагружен");
                    } else
                        player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                } else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Перезагрузить себе скин может только игрок");
            }
        }
        else if (args.length == 2) {
            switch (args[0]) {
                case "set": {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (player.hasPermission("kvadratutils.command.skin.set")) {
                            String title = args[1];
                            if (getAvailableSkins().contains(title)) {
                                saveSkinRelation(player, title);
                                Main.getReflector().setSkin(player, title);
//                                player.sendMessage(ChatColor.YELLOW + "Вам был присвоен скин " + ChatColor.GOLD + title);
                            } else
                                player.sendMessage(ChatColor.RED + "Неверное название скина");
                        } else
                            player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    } else
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Установить себе скин может только игрок");
                    break;
                }
                case "reset": {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (sender.hasPermission("kvadratutils.command.skin.reset") && (Objects.equals(sender, target) || sender.hasPermission("kvadratutils.command.skin.reset.others"))) {
                        if (target != null) {
                            resetSkinRelation(target);
                            sender.sendMessage(ChatColor.YELLOW + "Скин игрока " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " был сброшен");
                            target.sendMessage(ChatColor.YELLOW + "Ваш скин был сброшен.");
                        } else
                            sender.sendMessage(ChatColor.RED + "Игрок не найден");
                    } else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "reload": {
                    if (sender.hasPermission("kvadratutils.command.skin.reload.others")) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target != null) {
                            reloadSkin(target);
                            target.sendMessage(ChatColor.YELLOW + "Ваш скин был перезагружен");
                            sender.sendMessage(ChatColor.YELLOW + "Скин указаного игока перезагружен");
                        }
                        else
                            sender.sendMessage(ChatColor.RED + "Игрок не найден");
                    }
                    else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "delete": {
                    if (sender.hasPermission("kvadratutils.command.skin.delete")) {
                        String title = args[1];
                        if (getAvailableSkins().contains(title)) {
                            deleteSkin(title);
                            sender.sendMessage(ChatColor.GREEN + "Скин удалён из базы данных");
                        }
                        else
                            sender.sendMessage(ChatColor.RED + "Неверное название скина");
                    } else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "player": {
                    Player subjectPlayer = Bukkit.getPlayer(args[1]);
                    if (subjectPlayer == null) {
                        try {
                            subjectPlayer = Bukkit.getPlayer(UUID.fromString(args[1]));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(ChatColor.RED + "Игрок с указанным ником/UUID не найден");
                        }
                    }
                    if (subjectPlayer != null) {
                        Player finalSubjectPlayer = subjectPlayer;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                HashMap<Integer, byte[]> values = new HashMap<>();
                                values.put(1, Converter.uuidToBytes(finalSubjectPlayer.getUniqueId()));
                                if (Boolean.TRUE.equals(Main.makeExecuteQuery(
                                        new QueryBuilder().select(Main.PLAYER_SKINS_TABLE_NAME)
                                                .what("Skin_Value, Skin_Signature")
                                                .from()
                                                .where("Player = ?")
                                                .build(),
                                        values,
                                        skinResultSet -> {
                                            try {
                                                if (skinResultSet.next()) {
                                                    Main.makeExecuteUpdate(
                                                            new QueryBuilder().insert(Main.SKINS_TABLE_NAME)
                                                                    .setColumns("Name", "Skin_Value", "Skin_Signature")
                                                                    .setValues("'" + finalSubjectPlayer.getName() + "'", "'" + skinResultSet.getString(1) + "'", "'" + skinResultSet.getString(2) + "'")
                                                                    .onDuplicateKeyUpdate()
                                                                    .build(),
                                                            new HashMap<>()
                                                    );
                                                    return true;
                                                } else
                                                    return false;
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                                return false;
                                            }
                                        }
                                )))
                                    sender.sendMessage(ChatColor.GREEN + "Скин указанного игрока успешно скопирован в БД общедоступных скинов");
                                else
                                    sender.sendMessage(ChatColor.RED + "Скин указанного игрока не был найден или возникла другая ошибка");
                            }
                        }.runTaskAsynchronously(Main.getInstance());
                    } else
                        return false;
                    break;
                }
                default:
                    return false;
            }
        }
        else if (args.length >= 3) {
            switch (args[0]) {
                case "set": {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (sender.hasPermission("kvadratutils.command.skin.set") && (Objects.equals(target, sender) || sender.hasPermission("kvadratutils.command.skin.set.others"))) {
                        if (args.length == 3) {
                            String title = args[1];
                            if (target != null && getAvailableSkins().contains(title)) {
                                saveSkinRelation(target, title);
                                Main.getReflector().setSkin(target, title);
                                sender.sendMessage(ChatColor.YELLOW + "Игроку " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " был присвоен скин " + ChatColor.GOLD + title);
//                                target.sendMessage(ChatColor.YELLOW + "Вам был присвоен скин " + ChatColor.GOLD + title);
                            }
                            else if (target == null)
                                sender.sendMessage(ChatColor.RED + "Игрок не найден");
                            else if (!getAvailableSkins().contains(title))
                                sender.sendMessage(ChatColor.RED + "Неверное название скина");
                        }
                        else
                            return false;
                    }
                    else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "save": {
                    if (sender.hasPermission("kvadratutils.command.skin.save")) {
                        String title = !args[1].contains(" ") && args.length <= 256 ? args[1] : null;
                        URL imageURL;
                        Boolean isSlim = null;
                        try {
                            imageURL = new URL(args[2]);
                            if (title == null)
                                throw new DataFormatException("Неверный формат названия. Пробелы не разрешены. Максимальная разрешенная длинна = 256 символов");
                            if (args.length == 4)
                                if (args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("false"))
                                    isSlim = Boolean.parseBoolean(args[3]);
                                else throw new DataFormatException("Неверный формат булевого значения");
                            else if (args.length > 4)
                                return false;
                            Boolean finalIsSlim = isSlim;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (saveSkin(title, imageURL, finalIsSlim))
                                        sender.sendMessage(ChatColor.GREEN + "Скин был успешно сохранён");
                                    else
                                        sender.sendMessage(ChatColor.RED + "При сохранении возникла ошибка");
                                }
                            }.runTaskAsynchronously(Main.getInstance());
                        } catch (MalformedURLException | DataFormatException e) {
                            sender.sendMessage(ChatColor.RED + e.getMessage());
                        }
                    }
                    else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "player": {
                    Player subjectPlayer = Bukkit.getPlayer(args[1]);
                    if (subjectPlayer == null) {
                        try {
                            subjectPlayer = Bukkit.getPlayer(UUID.fromString(args[1]));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(ChatColor.RED + "Игрок с указанным ником/UUID не найден");
                        }
                    }
                    if (subjectPlayer != null) {
                        Player finalSubjectPlayer = subjectPlayer;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                HashMap<Integer, byte[]> values = new HashMap<>();
                                values.put(1, Converter.uuidToBytes(finalSubjectPlayer.getUniqueId()));
                                if (Boolean.TRUE.equals(Main.makeExecuteQuery(
                                        new QueryBuilder().select(Main.PLAYER_SKINS_TABLE_NAME)
                                                .what("Skin_Value, Skin_Signature")
                                                .from()
                                                .where("Player = ?")
                                                .build(),
                                        values,
                                        skinResultSet -> {
                                            try {
                                                if (skinResultSet.next()) {
                                                    Main.makeExecuteUpdate(
                                                            new QueryBuilder().insert(Main.SKINS_TABLE_NAME)
                                                                    .setColumns("Name", "Skin_Value", "Skin_Signature")
                                                                    .setValues("'" + args[2] + "'", "'" + skinResultSet.getString(1) + "'", "'" + skinResultSet.getString(2) + "'")
                                                                    .onDuplicateKeyUpdate()
                                                                    .build(),
                                                            new HashMap<>()
                                                    );
                                                    return true;
                                                } else
                                                    return false;
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                                return false;
                                            }
                                        }
                                )))
                                    sender.sendMessage(ChatColor.GREEN + "Скин указанного игрока успешно скопирован в БД общедоступных скинов");
                                else
                                    sender.sendMessage(ChatColor.RED + "Скин указанного игрока не был найден или возникла другая ошибка");
                            }
                        }.runTaskAsynchronously(Main.getInstance());
                    } else
                        return false;
                    break;
                }
                default:
                    return false;
            }
        }
        else if (
                sender.hasPermission("kvadratutils.command.skin.set") ||
                sender.hasPermission("kvadratutils.command.skin.save") ||
                sender.hasPermission("kvadratutils.command.skin.set.others") ||
                sender.hasPermission("kvadratutils.command.skin.reset") ||
                sender.hasPermission("kvadratutils.command.skin.reset.others") ||
                sender.hasPermission("kvadratutils.command.skin.delete")
        )
            return false;
        else
            sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        return true;
    }

    public static List<String> getAvailableSkins() {
        return Main.makeExecuteQuery(
                "SELECT Name FROM " + Main.SKINS_TABLE_NAME + ";",
                new HashMap<>(),
                skinsResultSet -> {
                    List<String> result = new ArrayList<>();
                    try {
                        while (skinsResultSet.next())
                            result.add(skinsResultSet.getString(1));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return result;
                });
    }

    @SuppressWarnings("deprecation")
    private static boolean saveSkin(@NotNull final String title, @NotNull final URL imageURL, @Nullable final Boolean isSlim) {
        try {
            String symbols = "abcdefghijklmnopqrstuvwxyz";
            StringBuilder name = new StringBuilder();
            for (byte i = 0; i < RandomContainer.r.nextInt(20) + 1; i++) {
                String c = String.valueOf(symbols.charAt(RandomContainer.r.nextInt(symbols.length())));
                if (RandomContainer.r.nextBoolean())
                    c = c.toUpperCase();
                name.append(c);
            }

            File skinsFolder = new File(Main.getInstance().getDataFolder(), "skins");
            if (!skinsFolder.exists())
                skinsFolder.mkdirs();

            File skinFile = new File(skinsFolder, name + ".png");

            try (
                    ReadableByteChannel rbc = Channels.newChannel(imageURL.openStream());
                    FileOutputStream fOutStream = new FileOutputStream(skinFile)
            ) {
                fOutStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            SkinOptions options;
            if (isSlim == null) options = new SkinOptions(
                    name.toString(),
                    Variant.AUTO,
                    Visibility.PUBLIC
            );
            else options = new SkinOptions(
                    name.toString(),
                    isSlim ? Variant.SLIM : Variant.CLASSIC,
                    Visibility.PUBLIC
            );

            Connection.Response response = Jsoup
                    .connect("https://api.mineskin.org/generate/upload?" + options.toUrlParam())
                    .userAgent("kvadratutils")
                    .header("Authorization", "b3e706213c3d29a88a1922ccc9c9c76a801dc111f0c24f5f7e2f41cd268ce4fc")
                    .method(Connection.Method.POST)
                    .data("file", skinFile.getName(), new FileInputStream(skinFile))
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(40000)
                    .execute();

            JsonReader reader = new JsonReader(new StringReader(response.body()));
            reader.setLenient(true);
            JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();

            Files.delete(skinFile.toPath());

            switch (response.statusCode()) {
                case 200: {
                    String value = jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("value").getAsString();
                    String signature = jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("signature").getAsString();
                    Main.makeExecuteUpdate(
                            new QueryBuilder().insert(Main.SKINS_TABLE_NAME)
                                    .setColumns("Name", "Skin_Value", "Skin_Signature")
                                    .setValues("'" + title + "'", "'" + value + "'", "'" + signature + "'")
                                    .onDuplicateKeyUpdate()
                                    .build(),
                            new HashMap<>()
                    );
                    Main.makeExecuteQuery(
                            new QueryBuilder().select(Main.SKIN_RELATION_TABLE_NAME)
                                    .what("Player")
                                    .from()
                                    .where("Skin_Name = '" + title + "'")
                                    .build(),
                            new HashMap<>(),
                            playersResultSet -> {
                                try {
                                    while (playersResultSet.next()) {
                                        Player player = Bukkit.getPlayer(Converter.uuidFromBytes(playersResultSet.getBytes(1)));
                                        if (player != null) {
                                            Main.getReflector().setSkin(player, title);
                                            player.sendMessage(ChatColor.YELLOW + "Ваш скин был обновлён");
                                        }
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                    );
                    return true;
                }
                case 400: {
                    throw new IllegalArgumentException(jsonResponse.getAsJsonObject("error").getAsString());
                }
                case 429: {
                    throw new TooSoonRequestException(
                            "\n" + jsonResponse.getAsJsonPrimitive("error").getAsString() +
                            "\n" + jsonResponse.getAsJsonPrimitive("nextRequest").getAsString() +
                            "\n" + jsonResponse.getAsJsonPrimitive("delay").getAsString()
                    );
                }
                case 500: {
                    throw new RemoteException("Server exception;\n" + jsonResponse.getAsJsonObject("error").getAsString());
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + response.statusCode());
            }
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void reloadSkin(@NotNull final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Connection.Response response = Jsoup
                            .connect("https://sessionserver.mojang.com/session/minecraft/profile/" + player.getUniqueId() + "?unsigned=false")
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute();
                    if (response.statusCode() == 200) {
                        JsonReader reader = new JsonReader(new StringReader(response.body()));
                        reader.setLenient(true);
                        JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();

                        JsonObject textureProperty = jsonResponse.getAsJsonArray("properties").get(0).getAsJsonObject();
                        String value = textureProperty.getAsJsonPrimitive("value").getAsString(),
                                signature = textureProperty.getAsJsonPrimitive("signature").getAsString();
                        Main.getReflector().setSkin(player, value, signature);

                        HashMap<Integer, byte[]> values = new HashMap<>();
                        values.put(1, Converter.uuidToBytes(player.getUniqueId()));
                        values.put(2, Converter.uuidToBytes(player.getUniqueId()));
                        Main.makeExecuteUpdate(new QueryBuilder().insert(Main.PLAYER_SKINS_TABLE_NAME)
                                        .setColumns("Player", "Skin_Value", "Skin_Signature")
                                        .setValues("?", "'" + value + "'", "'" + signature + "'")
                                        .onDuplicateKeyUpdate()
                                        .build(),
                                values);
                        resetSkinRelation(player);
                    }
                    else
                        Main.getInstance().getLogger().log(Level.ALL, response.body());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    private static void deleteSkin(@NotNull final String title) {
        Main.makeExecuteUpdate(
                "DELETE FROM " + Main.SKINS_TABLE_NAME + " WHERE Name = '" + title + "';",
                new HashMap<>());
        List<UUID> players = Main.makeExecuteQuery(
                new QueryBuilder()
                        .select(Main.SKIN_RELATION_TABLE_NAME)
                        .what("Player").from().where("Skin_Name = '" + title + "'").build(),
                new HashMap<>(),
                playersResultSet -> {
                    List<UUID> playersUUIDs = new ArrayList<>();
                    try {
                        while (playersResultSet.next())
                            playersUUIDs.add(Converter.uuidFromBytes(playersResultSet.getBytes(1)));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return playersUUIDs;
                });
        Main.makeExecuteUpdate(
                "DELETE FROM " + Main.SKIN_RELATION_TABLE_NAME + " WHERE Skin_Name = '" + title + "';",
                new HashMap<>());
        Iterator<Player> playerIterator = Objects.requireNonNull(players).stream().map(Bukkit::getPlayer).iterator();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerIterator.hasNext())
                    resetSkin(playerIterator.next());
                else
                    cancel();
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 10L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HashMap<Integer, byte[]> values = new HashMap<>();
        values.put(1, Converter.uuidToBytes(player.getUniqueId()));
        new BukkitRunnable() {
            @Override
            public void run() {
                String title = Main.makeExecuteQuery(
                        new QueryBuilder().select(Main.SKIN_RELATION_TABLE_NAME)
                                .what("Skin_Name")
                                .from()
                                .where("Player = ?")
                                .build(),
                        values,
                        skinNameResultSet -> {
                            try {
                                if (skinNameResultSet.next())
                                    return skinNameResultSet.getString(1);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                return null;
                            }
                            return null;
                        }
                );
                if (Boolean.FALSE.equals(Main.makeExecuteQuery(
                        new QueryBuilder().select(Main.SKINS_TABLE_NAME)
                                .what("*")
                                .from()
                                .where("Name = '" + title + "'")
                                .build(),
                        new HashMap<>(),
                        rs -> {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                e.printStackTrace();
                                return false;
                            }
                        }))) {
                    title = null;
                    resetSkinRelation(player);
                }
                if (title != null) {
                    Main.getReflector().setSkin(player, title);
                    player.sendMessage(ChatColor.YELLOW + "Сейчас активен скин " + ChatColor.GOLD + title);
                } else
                    player.sendMessage(ChatColor.YELLOW + "Сейчас активен ваш собственный скин");
            }
        }.runTaskLaterAsynchronously(Main.getInstance(), 20L);
    }

    public static void saveSkinRelation(@NotNull final Player player, @Nullable final String title) {
        HashMap<Integer, byte[]> values = new HashMap<>();
        values.put(1, Converter.uuidToBytes(player.getUniqueId()));
        values.put(2, Converter.uuidToBytes(player.getUniqueId()));
        Main.makeExecuteUpdate(
                new QueryBuilder().insert(Main.SKIN_RELATION_TABLE_NAME)
                        .setColumns("Player", "Skin_Name")
                        .setValues("?", "'" + title + "'")
                        .onDuplicateKeyUpdate()
                        .build(),
                values);
    }

    public static void resetSkinRelation(@NotNull final Player player) {
        HashMap<Integer, byte[]> values = new HashMap<>();
        values.put(1, Converter.uuidToBytes(player.getUniqueId()));
        Main.makeExecuteUpdate("DELETE FROM " + Main.SKIN_RELATION_TABLE_NAME + " WHERE Player = ?;", values);

        resetSkin(player);
    }

    @SuppressWarnings("deprecation")
    public static void resetSkin(@NotNull final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    AtomicReference<String> value = new AtomicReference<>();
                    AtomicReference<String> signature = new AtomicReference<>();
                    HashMap<Integer, byte[]> values = new HashMap<>();
                    values.put(1, Converter.uuidToBytes(player.getUniqueId()));
                    Main.makeExecuteQuery(
                            new QueryBuilder().select(Main.PLAYER_SKINS_TABLE_NAME)
                                    .what("Skin_Value, Skin_Signature")
                                    .from()
                                    .where("Player = ?")
                                    .build(),
                            values,
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

                    if (value.get() == null || signature.get() == null) {
                        Connection.Response response = Jsoup
                                .connect("https://api.mineskin.org/generate/user?name=" + player.getName() + "&uuid=" + player.getUniqueId()/*.toString().replace("-", "")*/)
                                .userAgent("kvadratutils")
                                .header("Authorization", "b3e706213c3d29a88a1922ccc9c9c76a801dc111f0c24f5f7e2f41cd268ce4fc")
                                .method(Connection.Method.POST)
                                .ignoreContentType(true)
                                .ignoreHttpErrors(true)
                                .timeout(40000)
                                .execute();
                        JsonReader reader = new JsonReader(new StringReader(response.body()));
                        reader.setLenient(true);
                        JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();

                        if (response.statusCode() == 200) {
                            value.set(jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("value").getAsString());
                            signature.set(jsonResponse.getAsJsonObject("data").getAsJsonObject("texture").getAsJsonPrimitive("signature").getAsString());
                            Main.getReflector().setSkin(player, value.get(), signature.get());
                            values.put(2, Converter.uuidToBytes(player.getUniqueId()));
                            Main.makeExecuteUpdate(
                                    new QueryBuilder().insert(Main.PLAYER_SKINS_TABLE_NAME)
                                            .setColumns("Player", "Skin_Value", "Skin_Signature")
                                            .setValues("?", "'" + value.get() + "'", "'" + signature.get() + "'")
                                            .onDuplicateKeyUpdate()
                                            .build(),
                                    values);
                        } else
                            Main.getInstance().getLogger().log(Level.ALL, jsonResponse.toString());
                    }
                    else
                        Main.getReflector().setSkin(player, value.get(), signature.get());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    private static class TooSoonRequestException extends RuntimeException {
        TooSoonRequestException(String message) {
            super("The request was sent too soon; " + message);
        }
    }


    // Tab completion

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1)
                return StringUtil.copyPartialMatches(
                        args[0],
                        Stream.of(
                                player.hasPermission("kvadratutils.command.skin.set") ? "set" : null,
                                player.hasPermission("kvadratutils.command.skin.save") ? "save" : null,
                                player.hasPermission("kvadratutils.command.skin.reset") ? "reset" : null,
                                player.hasPermission("kvadratutils.command.skin.delete") ? "delete" : null,
                                player.hasPermission("kvadratutils.command.skin.save") ? "player" : null,
                                player.hasPermission("kvadratutils.command.skin.reload") ? "reload" : null
                        ).filter(Objects::nonNull).collect(Collectors.toList()),
                        new ArrayList<>());
            if (args.length == 2) {
                if (args[0].equals("set") && player.hasPermission("kvadratutils.command.skin.set"))
                    return copyPartialInnerMatches(args[1], getAvailableSkins());
                else if (args[0].equals("save") && player.hasPermission("kvadratutils.command.skin.save"))
                    return args[1].length() == 0 ? Collections.singletonList("<title> <image_url>") : null;
                else if (args[0].equals("reset") && player.hasPermission("kvadratutils.command.skin.reset.others"))
                    return copyPartialInnerMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                else if (args[0].equals("reload") && player.hasPermission("kvadratutils.command.skin.reload.others"))
                    return copyPartialInnerMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                else if (args[0].equals("delete") && player.hasPermission("kvadratutils.command.skin.delete"))
                    return copyPartialInnerMatches(args[1], getAvailableSkins());
                else if (args[0].equals("player") && player.hasPermission("kvadratutils.command.skin.save")) {
                    List<String> options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 6d, e -> e instanceof Player);
                    if (rayTraceResult != null && rayTraceResult.getHitEntity() != null)
                        options.add(rayTraceResult.getHitEntity().getName());
                    return copyPartialInnerMatches(args[1], options);
                }
            }
            if (args.length == 3) {
                if (args[0].equals("set") && player.hasPermission("kvadratutils.command.skin.set") && player.hasPermission("kvadratutils.command.skin.set.others"))
                    return copyPartialInnerMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                if (args[0].equals("save") && player.hasPermission("kvadratutils.command.skin.save"))
                    return args[2].length() == 0 ? Collections.singletonList("<image_url>") : null;
                else if (args[0].equals("player") && player.hasPermission("kvadratutils.command.skin.save"))
                    return args[2].length() == 0 ? Collections.singletonList("[title]") : null;
            }
        }
        return null;
    }

    private static List<String> copyPartialInnerMatches(final String lookFor, final @NotNull Collection<String> lookIn) {
        return lookIn.stream().filter(s -> s.contains(lookFor)).collect(Collectors.toList());
    }
}