package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import com.danikvitek.kvadratutils.utils.gui.PageUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

public class SkinSelectCommand implements CommandExecutor {
    private static final HashMap<UUID, Integer> pages = new HashMap<>();
    private static final ItemStack reloadSkinHead;

    static {
        reloadSkinHead = getReloadSkinHead();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.command.skin_select")) {
                Inventory skinInventory = Bukkit.createInventory(null, 54, "Выбор скина");
                Menu skinMenu = new Menu(skinInventory);
                pages.put(player.getUniqueId(), 0);

                redrawMenu(player, skinMenu, false);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private static void redrawMenu(Player player, Menu skinMenu, boolean reload) {
        List<ItemStack> skinIcons = getSkinIcons();
        setSkinIcons(player, skinMenu, skinIcons);
        setPageControls(player, skinMenu, skinIcons);
        if (reload)
            Main.getMenuHandler().reloadMenu(player);
        else {
            Main.getMenuHandler().closeMenu(player);
            Main.getMenuHandler().openMenu(player, skinMenu);
        }
    }

    private static void setPageControls(Player player, @NotNull Menu skinMenu, List<ItemStack> skinIcons) {
        skinMenu.setButton(45, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), pages.get(player.getUniqueId()) <= 0 ? PageUtil.getMaxPages(skinIcons, 45) - 1 : pages.get(player.getUniqueId()) - 1);
                redrawMenu(player, skinMenu, true);
            }
        });
        skinMenu.setButton(49, new Button(ControlButtons.QUIT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.remove(player.getUniqueId());
                Main.getMenuHandler().closeMenu(player);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.performCommand("menus");
                    }
                }.runTaskLater(Main.getPlugin(Main.class), 2L);
            }
        });
        skinMenu.setButton(53, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), (pages.get(player.getUniqueId()) + 1) % PageUtil.getMaxPages(skinIcons, 45));
                redrawMenu(player, skinMenu, true);
            }
        });
    }

    private static void setSkinIcons(@NotNull Player player, Menu skinMenu, List<ItemStack> skinIcons) {
        List<ItemStack> pageIcons = PageUtil.getPageItems(skinIcons, pages.get(player.getUniqueId()), 45);
        for (int i = 0; i < pageIcons.size(); i++) {
            int _i = i;
            skinMenu.setButton(i, new Button(pageIcons.get(i)) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                    Main.getMenuHandler().closeMenu(player);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String title = Objects.requireNonNull(pageIcons.get(_i).getItemMeta()).getDisplayName();
                            SkinCommand.saveSkinRelation(player, title);
                            Main.getReflector().setSkin(player, title);
//                            player.sendMessage(ChatColor.YELLOW + "Вам был присвоен скин " + ChatColor.GOLD + title);
                        }
                    }.runTaskAsynchronously(Main.getPlugin(Main.class));
                }
            });
        }
        skinMenu.setButton(47, new Button(reloadSkinHead) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                if (player.hasPermission("kvadratutils.command.skin.reload")) {
                    SkinCommand.reloadSkin(player);
                    player.sendMessage(ChatColor.YELLOW + "Ваш скин перезагружен");
                }
            }
        });
        skinMenu.setButton(48, new Button(new ItemBuilder(Material.CACTUS).setDisplayName("Сбросить скин").build()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Main.getMenuHandler().closeMenu(player);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        SkinCommand.resetSkinRelation(player);
                        player.sendMessage(ChatColor.YELLOW + "Ваш скин был сброшен");
                    }
                }.runTaskAsynchronously(Main.getPlugin(Main.class));
            }
        });
        skinMenu.setButton(50, new Button(getCurrentSkinHead(player)) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                redrawMenu(player, skinMenu, true);
            }
        });
    }

    private static List<ItemStack> getSkinIcons() {
        return Main.makeExecuteQuery(
                new QueryBuilder().select(Main.skinsTableName).what("Name, Skin_Value").from().build(),
                new HashMap<>(),
                skinsResultSet -> {
                    List<ItemStack> result = new ArrayList<>();
                    try {
                        while (skinsResultSet.next()) {
                            ItemStack skinHead = new ItemBuilder(Material.PLAYER_HEAD).setDisplayName(skinsResultSet.getString(1)).build();
                            SkullMeta meta = (SkullMeta) skinHead.getItemMeta();
                            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), null);
                            gameProfile.getProperties().put("textures", new Property("textures", skinsResultSet.getString(2)));

                            Field field;
                            assert meta != null;
                            field = meta.getClass().getDeclaredField("profile");
                            field.setAccessible(true);
                            field.set(meta, gameProfile);

                            skinHead.setItemMeta(meta);
                            result.add(skinHead);
                        }
                    } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return result;
                }
        );
    }

    private static ItemStack getCurrentSkinHead(Player player) {
        ItemStack currentSkinHead = new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("Текущий скин").build();
        SkullMeta meta = (SkullMeta) currentSkinHead.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        Property playerTextureProperty = Main.getReflector().getTextureProperty(player);
        profile.getProperties().put("textures", playerTextureProperty);
        Field field;
        try {
            assert meta != null;
            field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        currentSkinHead.setItemMeta(meta);

        return currentSkinHead;
    }

    private static ItemStack getReloadSkinHead() {
        //give @p skull 1 3 {display:{Name:"Action Repeat"},SkullOwner:{Id:"870abaa4-4183-4aed-a527-b943a2f334c2",Properties:{textures:[{Value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTUzZGQ0NTc5ZWRjMmE2ZjIwMzJmOTViMWMxODk4MTI5MWI2YzdjMTFlYjM0YjZhOGVkMzZhZmJmYmNlZmZmYiJ9fX0="}]}}}
        ItemStack reloadSkinHead = new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("Перезагрузить скин").build();
        SkullMeta meta = (SkullMeta) reloadSkinHead.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        Property playerTextureProperty = new Property("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTUzZGQ0NTc5ZWRjMmE2ZjIwMzJmOTViMWMxODk4MTI5MWI2YzdjMTFlYjM0YjZhOGVkMzZhZmJmYmNlZmZmYiJ9fX0=", null);
        profile.getProperties().put("textures", playerTextureProperty);
        Field field;
        try {
            assert meta != null;
            field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        reloadSkinHead.setItemMeta(meta);

        return reloadSkinHead;
    }
}