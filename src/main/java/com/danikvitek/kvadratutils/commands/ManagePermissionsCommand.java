package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import com.danikvitek.kvadratutils.utils.gui.PageUtil;
import net.luckperms.api.node.types.PermissionNode;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ManagePermissionsCommand implements CommandExecutor {
    private static final HashMap<UUID, Integer> pages = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.command.manage_permissions")) {
                Inventory managerInventory = Bukkit.createInventory(null, 54, "Менеджер разрешений");
                Menu managerMenu = new Menu(managerInventory);
                pages.put(player.getUniqueId(), 0);

                redrawMenu(player, managerMenu, false);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private static void redrawMenu(Player player, Menu managerMenu, boolean reload) {
        setPageControls(player, managerMenu);
        setPlayerHeads(player, managerMenu);
        if (reload)
            Main.getMenuHandler().reloadMenu(player);
        else {
            Main.getMenuHandler().closeMenu(player);
            Main.getMenuHandler().openMenu(player, managerMenu);
        }
    }

    private static void setPlayerHeads(Player player, Menu managerMenu) {
        List<ItemStack> pageHeads = PageUtil.getPageItems(getPlayerHeads(), pages.get(player.getUniqueId()), 45);
        for (int i = 0; i < pageHeads.size(); i ++) {
            int _i = i;
            Player subjectPlayer = Bukkit.getPlayer(Objects.requireNonNull(pageHeads.get(_i).getItemMeta()).getDisplayName());
            if (subjectPlayer != null && (!subjectPlayer.hasPermission("kvadratutils.moderator") || player.hasPermission("kvadratutils.moderator"))) {
                managerMenu.setButton(i, new Button(pageHeads.get(_i)) {
                    private int page = 0;
                    private final Player subjectPlayer = Bukkit.getPlayer(Objects.requireNonNull(pageHeads.get(_i).getItemMeta()).getDisplayName());

                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        if (subjectPlayer != null) {
                            Inventory permissionsInventory = Bukkit.createInventory(null, 27, "Разрешения для " + subjectPlayer.getName());
                            Menu permissionsMenu = new Menu(permissionsInventory);
                            page = 0;

                            redrawMenu(permissionsMenu, false);
                        } else
                            ManagePermissionsCommand.redrawMenu(player, managerMenu, true);
                    }

                    private void redrawMenu(Menu permissionsMenu, boolean reload) {
                        setPageControls(permissionsMenu);
                        setIcons(permissionsMenu);
                        setToggleControls(permissionsMenu);
                        if (reload)
                            Main.getMenuHandler().reloadMenu(player);
                        else {
                            Main.getMenuHandler().closeMenu(player);
                            Main.getMenuHandler().openMenu(player, permissionsMenu);
                        }
                    }

                    private void setPageControls(Menu permissionsMenu) {
                        permissionsMenu.setButton(18, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
                            @Override
                            public void onClick(Menu menu, InventoryClickEvent event) {
                                event.setCancelled(true);
                                page = page <= 0 ? PageUtil.getMaxPages(getPermissionIcons(), 9) - 1 : page - 1;
                                redrawMenu(permissionsMenu, true);
                            }
                        });
                        permissionsMenu.setButton(22, new Button(ControlButtons.QUIT.getItemStack()) {
                            @Override
                            public void onClick(Menu menu, InventoryClickEvent event) {
                                event.setCancelled(true);
                                Main.getMenuHandler().closeMenu(player);
                                Main.getMenuHandler().openMenu(player, managerMenu);
                            }
                        });
                        permissionsMenu.setButton(26, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
                            @Override
                            public void onClick(Menu menu, InventoryClickEvent event) {
                                event.setCancelled(true);
                                page = (page + 1) % PageUtil.getMaxPages(getPermissionIcons(), 9);
                                redrawMenu(permissionsMenu, true);
                            }
                        });
                    }

                    private void setIcons(Menu permissionsMenu) {
                        List<ItemStack> icons = PageUtil.getPageItems(getPermissionIcons(), page, 9);
                        for (int i = 0; i < 9; i++) {
                            permissionsMenu.setButton(i, new Button(i < icons.size() ? icons.get(i) : null) {
                                @Override
                                public void onClick(Menu menu, InventoryClickEvent event) {
                                    event.setCancelled(true);
                                    redrawMenu(permissionsMenu, true);
                                }
                            });
                        }
                    }

                    private void setToggleControls(Menu permissionsMenu) {
                        // todo: implement LuckPerms
                        List<String> permissions = PageUtil.getPageItems(getPermissionIcons(), page, 9)
                                .stream().map(ItemStack::getItemMeta).map(m -> {
                                    assert m != null;
                                    return m.getLore();
                                }).map(l -> {
                                    assert l != null;
                                    return ChatColor.stripColor(l.get(1));
                                }).collect(Collectors.toList());
                        for (int i = 9; i < 18; i++) {
                            int _i = i;
                            permissionsMenu.setButton(i, new Button(i - 9 < permissions.size() ? (subjectPlayer.hasPermission(permissions.get(i - 9))
                                    ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "Разрешено").build()
                                    : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "Запрещено").build()) : null) {
                                @Override
                                public void onClick(Menu menu, InventoryClickEvent event) {
                                    event.setCancelled(true);
                                    if (_i - 9 < permissions.size()) {
                                        PermissionNode permissionNode = PermissionNode.builder(permissions.get(_i - 9)).value(!subjectPlayer.hasPermission(permissions.get(_i - 9))).build();
                                        Main.getLuckPermsAPI().getUserManager().modifyUser(subjectPlayer.getUniqueId(),
                                                u -> u.data().add(permissionNode)
                                        );
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                redrawMenu(permissionsMenu, true);
                                            }
                                        }.runTaskLater(Main.getPlugin(Main.class), 2L);
                                    }
                                    else
                                        redrawMenu(permissionsMenu, true);
                                }
                            });
                        }

                        permissionsMenu.setButton(21, new Button(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName(ChatColor.RED + "Запретить всё").build()) {
                            @Override
                            public void onClick(Menu menu, InventoryClickEvent event) {
                                event.setCancelled(true);
                                for (String permission : PageUtil.getPageItems(getPermissionIcons(), page, 9)
                                        .stream().map(ItemStack::getItemMeta).map(m -> {
                                            assert m != null;
                                            return m.getLore();
                                        }).map(l -> {
                                            assert l != null;
                                            return ChatColor.stripColor(l.get(1));
                                        }).collect(Collectors.toList())) {
                                    Main.getLuckPermsAPI().getUserManager().modifyUser(subjectPlayer.getUniqueId(),
                                            u -> u.data().add(PermissionNode.builder(permission).value(false).build())
                                    );
                                }
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        redrawMenu(permissionsMenu, true);
                                    }
                                }.runTaskLater(Main.getPlugin(Main.class), 5L);
                            }
                        });
                        permissionsMenu.setButton(23, new Button(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName(ChatColor.GREEN + "Разрешить всё").build()) {
                            @Override
                            public void onClick(Menu menu, InventoryClickEvent event) {
                                event.setCancelled(true);
                                for (String permission : PageUtil.getPageItems(getPermissionIcons(), page, 9)
                                        .stream().map(ItemStack::getItemMeta).map(m -> {
                                            assert m != null;
                                            return m.getLore();
                                        }).map(l -> {
                                            assert l != null;
                                            return ChatColor.stripColor(l.get(1));
                                        }).collect(Collectors.toList())) {
                                    Main.getLuckPermsAPI().getUserManager().modifyUser(subjectPlayer.getUniqueId(),
                                            u -> u.data().add(PermissionNode.builder(permission).value(true).build())
                                    );
                                }
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        redrawMenu(permissionsMenu, true);
                                    }
                                }.runTaskLater(Main.getPlugin(Main.class), 5L);
                            }
                        });
                    }

                    private List<ItemStack> getPermissionIcons() {
                        return Arrays.asList(
                                new ItemBuilder(Material.PAINTING).setDisplayName(ChatColor.GOLD + "Использовать меню").setLore("", ChatColor.YELLOW + "kvadratutils.command.menus").build(),
                                new ItemBuilder(Material.ITEM_FRAME).setDisplayName(ChatColor.GOLD + "Использовать F3+N и F3+F4").setLore("", ChatColor.YELLOW + "kvadratutils.f3n_f3f4").build(),
                                new ItemBuilder(Material.GRASS_BLOCK).setDisplayName(ChatColor.GOLD + "использовать /gamemode").setLore("", ChatColor.YELLOW + "minecraft.command.gamemode").build(),
                                new ItemBuilder(Material.BEE_SPAWN_EGG).setDisplayName(ChatColor.GOLD + "Менеджер сущностей").setLore("", ChatColor.YELLOW + "kvadratutils.command.entity_manager").build(),
                                new ItemBuilder(Material.COMMAND_BLOCK).setDisplayName(ChatColor.GOLD + "Менеджер командных блоков").setLore("", ChatColor.YELLOW + "kvadratutils.command.command_blocks").build(),
                                new ItemBuilder(MenusCommand.getPlayerHead()).setDisplayName(ChatColor.GOLD + "Установка скинов").setLore("", ChatColor.YELLOW + "kvadratutils.command.skin_select").build(),
                                new ItemBuilder(Material.ENDER_PEARL).setDisplayName(ChatColor.GOLD + "Меню телепортации").setLore("", ChatColor.YELLOW + "kvadratutils.command.tp_menu").build(),
                                new ItemBuilder(Material.PLAYER_HEAD).setOwner("MHF_ArrowDown").setDisplayName(ChatColor.GOLD + "Телепортация к тебе").setLore("", ChatColor.YELLOW + "kvadratutils.teleport_to_player." + player.getName()).build(),
                                new ItemBuilder(Material.PLAYER_HEAD).setOwner("MHF_ArrowUp").setDisplayName(ChatColor.GOLD + "Телепортация тебя").setLore("", ChatColor.YELLOW + "kvadratutils.teleport_player." + player.getName()).build(),
                                new ItemBuilder(Material.WRITABLE_BOOK).setDisplayName(ChatColor.GOLD + "Менеджер разрешений").setLore("", ChatColor.YELLOW + "kvadratutils.command.manage_permissions").build(),
                                new ItemBuilder(Material.CARVED_PUMPKIN).setDisplayName(ChatColor.GOLD + "Одевать на голову блоки").setLore("", ChatColor.YELLOW + "hat.blocks").build(),
                                new ItemBuilder(Material.CARVED_PUMPKIN).setDisplayName(ChatColor.GOLD + "Одевать на голову предметы").setLore("", ChatColor.YELLOW + "hat.items").build(),
                                new ItemBuilder(Material.PHANTOM_MEMBRANE).setDisplayName(ChatColor.GOLD + "Открывать /cmenu").setLore("", ChatColor.YELLOW + "cmenu.show").build()
                                // todo: add more
                        );
                    }
                });
            }
        }
    }

    // 45, 49, 53
    private static void setPageControls(Player player, Menu managerMenu) {
        managerMenu.setButton(45, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), pages.get(player.getUniqueId()) <= 0 ? PageUtil.getMaxPages(getPlayerHeads(), 45) - 1 : pages.get(player.getUniqueId()) - 1);
                redrawMenu(player, managerMenu, true);
            }
        });
        managerMenu.setButton(49, new Button(ControlButtons.QUIT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Main.getMenuHandler().closeMenu(player);
                pages.remove(player.getUniqueId());
            }
        });
        managerMenu.setButton(53, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), (pages.get(player.getUniqueId()) + 1) % PageUtil.getMaxPages(getPlayerHeads(), 45));
                redrawMenu(player, managerMenu, true);
            }
        });
    }

    private static List<ItemStack> getPlayerHeads() {
        return Bukkit.getOnlinePlayers().stream()
                .map(p ->
                        new ItemBuilder(Material.PLAYER_HEAD)
                                .setDisplayName(p.getName())
                                .setOwner(p.getName())
                                .build())
                .collect(Collectors.toList());
    }
}
