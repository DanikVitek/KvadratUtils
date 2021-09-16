package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.Menu;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.UUID;

public class MenusCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = ((Player) sender);
            if (player.hasPermission("kvadratutils.command.menus")) {
                Inventory menuInventory = Bukkit.createInventory(null, 45, "Выбор меню");
                Menu menusMenu = new Menu(menuInventory);
                /*
                    4

                20  22  24
                    31
                    40
                 */

                menusMenu.setButton(4, new Button(new ItemBuilder(Material.BEE_SPAWN_EGG).setDisplayName("Диспетчер сущностей").build()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.performCommand("entity_manager");
                    }
                });

                menusMenu.setButton(20, new Button(new ItemBuilder(Material.ENDER_PEARL).setDisplayName("Телепорт к игрокам").build()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.performCommand("tp_menu");
                    }
                });

                menusMenu.setButton(22, new Button(getPlayerHead()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.performCommand("skin_select");
                    }
                });

                menusMenu.setButton(24, new Button(new ItemBuilder(Material.WOODEN_AXE).setDisplayName("World Edit").addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        // TODO: 15.09.2021 do something with this
                    }
                });

                if (player.hasPermission("kvadratutils.command.manage_permissions"))
                    menusMenu.setButton(31, new Button(new ItemBuilder(Material.WRITABLE_BOOK).setDisplayName("Выдача разрешений").build()) {
                        @Override
                        public void onClick(Menu menu, InventoryClickEvent event) {
                            event.setCancelled(true);
                            player.performCommand("manage_permissions");
                        }
                    });

                menusMenu.setButton(40, new Button(new ItemBuilder(Material.COMMAND_BLOCK).setDisplayName("Командные блоки").build()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.performCommand("command_blocks");
                    }
                });

                Main.getMenuHandler().openMenu(player, menusMenu);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private static ItemStack getPlayerHead() {
        ItemStack setSkinPlayerHead = new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("Установить скин").build();
        SkullMeta meta = (SkullMeta) setSkinPlayerHead.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYzMTc5ODQwNTUzMiwKICAicHJvZmlsZUlkIiA6ICI4MGFiMWFkMTgyMzU0NDFkYjhlYTMzNzQ2OTZkMWU0YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJWZW50b3JfUHJveHkiLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDViMWVlYmIwNWE3MWY0NmM2ZjY2OTBmNjk5NzU1NTExMWQ5ZWQ1YzViZTFlNjgyODk0NDZkMTg5ZjdlMWFiYyIKICAgIH0KICB9Cn0="));

        Field field;
        try {
            assert meta != null;
            field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        setSkinPlayerHead.setItemMeta(meta);

        return setSkinPlayerHead;
    }
}
