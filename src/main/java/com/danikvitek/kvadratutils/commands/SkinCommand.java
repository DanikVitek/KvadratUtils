package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

public class SkinCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // skin <set <title> [player]> | <save <title> <image_url> [is_slim]>
        if (args.length == 2 && args[0].equals("set")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("kvadratutils.command.skin.set")) {

                }
                else
                    player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
            }
            else
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Установить скин может только игрок");
        }
        else if (args.length >= 3) {
            switch (args[0]) {
                case "set": {
                    if (sender.hasPermission("kvadratutils.command.skin.set")) {

                    }
                    else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                case "save": {
                    if (sender.hasPermission("kvadratutils.command.skin.save")) {
                        String title = args[1];
                        URL imageURL;
                        Boolean isSlim = null;
                        try {
                            imageURL = new URL(args[2]);
                            if (args.length == 4)
                                if (args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("false"))
                                    isSlim = Boolean.parseBoolean(args[3]);
                                else throw new DataFormatException("Wrong boolean format");
                            else if (args.length > 4)
                                return false;
                            Boolean finalIsSlim = isSlim;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    saveSkin(title, imageURL, finalIsSlim);
                                }
                            }.runTaskAsynchronously(Main.getPlugin(Main.class));
                        } catch (MalformedURLException | DataFormatException e) {
                            sender.sendMessage(e.getMessage());
                        }
                    }
                    else
                        sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
                    break;
                }
                default:
                    return false;
            }
        }
        else if (sender.hasPermission("kvadratutils.command.skin.set") || sender.hasPermission("kvadratutils.command.skin.save"))
            return false;
        else
            sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        return true;
    }

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
                                player.hasPermission("kvadratutils.command.skin.save") ? "save" : null
                        ).filter(Objects::nonNull).collect(Collectors.toList()),
                        new ArrayList<>());
            if (args.length == 2) {
                if (args[0].equals("set") && player.hasPermission("kvadratutils.command.skin.set"))
                    return copyPartialInnerMatches(args[1], getAvailableSkins());
                else if (args[0].equals("save") && player.hasPermission("kvadratutils.command.skin.save"))
                    return args[1].length() == 0 ? Collections.singletonList("<title> <image_url>") : null;
            }
            if (args.length == 3) {
                if (args[0].equals("set") && player.hasPermission("kvadratutils.command.skin.set"))
                    return copyPartialInnerMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                if (args[0].equals("save") && player.hasPermission("kvadratutils.command.skin.save"))
                    return args[2].length() == 0 ? Collections.singletonList("<image_url>") : null;
            }
        }
        return null;
    }

    public static List<String> getAvailableSkins() {
        return Main.makeExecuteQuery(
                "SELECT Name FROM " + Main.skinsTableName + ";",
                new HashMap<>(),
                (args, skinsResultSet) -> {
                    List<String> result = new ArrayList<>();
                    try {
                        while (skinsResultSet.next())
                            result.add(skinsResultSet.getString(1));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return result;
                }, null);
    }

    private static void saveSkin(String title, URL imageURL, @Nullable Boolean isSlim) {
        try {
            String symbols = "abcdefghijklmnopqrstuvwxyz";
            StringBuilder name = new StringBuilder();
            for (byte i = 0; i < new Random().nextInt(20) + 1; i++) {
                String c = String.valueOf(symbols.charAt(new Random().nextInt(symbols.length())));
                if (new Random().nextBoolean())
                    c = c.toUpperCase();
                name.append(c);
            }


            InputStream in = new BufferedInputStream(imageURL.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = in.read(buf))) out.write(buf, 0, n);
            out.close();
            in.close();
            byte[] result = out.toByteArray();
            String file = Base64.getEncoder().encodeToString(result);

            JsonObject jsonBody = new JsonObject();
            if (isSlim != null)
                jsonBody.addProperty("variant", isSlim ? "slim" : "classic");
//            jsonBody.addProperty("url", imageURL.toString());
            jsonBody.addProperty("name", name.toString());
            jsonBody.addProperty("file", file);
            System.out.println(jsonBody);

            Connection.Response response = Jsoup
//                    .connect("https://api.mineskin.org/generate/url")
                    .connect("https://api.mineskin.org/generate/upload")
                    .userAgent("kvadratutils")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method(Connection.Method.POST)
                    .requestBody(jsonBody.toString())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();

            System.out.println(response.statusCode());

            JsonReader reader = new JsonReader(new StringReader(response.body()));
            reader.setLenient(true);
            JsonObject jsonResponse = new JsonParser().parse(reader).getAsJsonObject();
            System.out.println(jsonResponse.toString());
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> copyPartialInnerMatches(String lookFor, Collection<String> lookIn) {
        return lookIn.stream().filter(s -> s.contains(lookFor)).collect(Collectors.toList());
    }
}
