package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.command_blocks.CommandBlockInstance;
import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandBlocksCommand implements CommandExecutor {
    private static final HashMap<UUID, Integer> pages = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.moderator") ||
                player.hasPermission("kvadratutils.command.command_blocks")) {
                Inventory cbInventory = Bukkit.createInventory(null, 54, "Командные блоки");
                Menu cbMenu = new Menu(cbInventory);
                pages.put(player.getUniqueId(), 0);

                redrawMenu(player, cbMenu, false);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private void redrawMenu(Player player, Menu cbMenu, boolean reload) {
        final boolean[] future = { false };
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    List<ItemStack> allCBs = getAllCBs(player).get();

                    setPageControls(cbMenu, player, allCBs);
                    setCBControls(cbMenu, player, allCBs);
                    future[0] = true;
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (future[0]) {
                    if (reload)
                        Main.getMenuHandler().reloadMenu(player);
                    else {
                        Main.getMenuHandler().closeMenu(player);
                        Main.getMenuHandler().openMenu(player, cbMenu);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void setCBControls(Menu cbMenu, Player player, List<ItemStack> allCBs) {
        List<ItemStack> blocksOnPage = PageUtil.getPageItems(allCBs, pages.get(player.getUniqueId()), 45);
        for (int i = 0; i < blocksOnPage.size(); i++) {
            ItemStack cb = blocksOnPage.get(i);
            String pos = ChatColor.stripColor(Objects.requireNonNull(Objects.requireNonNull(cb.getItemMeta()).getLore()).get(2)).substring(12);
            int x = Integer.parseInt(pos.split(", ")[0]),
                y = Integer.parseInt(pos.split(", ")[1]),
                z = Integer.parseInt(pos.split(", ")[2]);
            cbMenu.setButton(i, new Button(cb) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
//                    Main.getMenuHandler().closeMenu(player);
//                    Main.getReflector().sendOpenCBGUI(player, Objects.requireNonNull(Bukkit.getWorld(ChatColor.stripColor(Objects.requireNonNull(Objects.requireNonNull(cb.getItemMeta()).getLore()).get(1)).substring(5))).getBlockAt(x, y, z));
                }
            });
        }
    }

    private void setPageControls(Menu cbMenu, Player player, List<ItemStack> allCBs) {
        cbMenu.setButton(45, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(
                        player.getUniqueId(),
                        pages.get(player.getUniqueId()) == 0
                                ? PageUtil.getMaxPages(allCBs, 45) - 1
                                : pages.get(player.getUniqueId()) - 1);
                redrawMenu(player, cbMenu, true);
            }
        });
        cbMenu.setButton(53, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), (pages.get(player.getUniqueId()) + 1) % PageUtil.getMaxPages(allCBs, 45));
                redrawMenu(player, cbMenu, true);
            }
        });
        cbMenu.setButton(49, new Button(ControlButtons.QUIT.getItemStack()) {
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
                }.runTaskLater(Main.getInstance(), 2L);
            }
        });
    }

    private CompletableFuture<List<ItemStack>> getAllCBs(Player player) {
        CompletableFuture<List<ItemStack>> allCBsCompletableFuture = new CompletableFuture<>();

        List<ItemStack> commandBlocks = new ArrayList<>();
        List<CommandBlockInstance> cbi = new ArrayList<>(CommandBlockInstance.getCommandBlockInstances());

        cbi.sort(Comparator.comparingDouble(cb -> cb.getPosition().distance(player.getLocation().toVector())));

        for (CommandBlockInstance commandBlockInstance : cbi) {
            World world = Bukkit.getWorld(commandBlockInstance.getWorld());
            List<String> lore = new ArrayList<>(Arrays.asList(
                    ChatColor.GOLD + "Условность: " + ChatColor.YELLOW + (commandBlockInstance.isConditional() ? "Условный" : "Безусловный"),
                    ChatColor.GOLD + "Мир: " + ChatColor.YELLOW + (world != null ? world.getName() : "Null"),
                    ChatColor.GOLD + "Координаты: " + ChatColor.YELLOW + commandBlockInstance.getPosition().getBlockX() + ", " + commandBlockInstance.getPosition().getBlockY() + ", " + commandBlockInstance.getPosition().getBlockZ(),
                    ChatColor.GOLD + "Команд выполнено: " + ChatColor.YELLOW + commandBlockInstance.getCommandsExecuted(),
                    ChatColor.GOLD + "Команда:"));
            String command = commandBlockInstance.getCommand();
            while (command.length() > 0) {
                StringBuilder rowBuilder = new StringBuilder();
                while (MinecraftFont.Font.getWidth(rowBuilder.toString()) < 200 && command.length() > 0) {
                    rowBuilder.append(command.charAt(0));
                    command = command.substring(1);
                }
                lore.add(ChatColor.YELLOW + rowBuilder.toString());
            }
            commandBlocks.add(new ItemBuilder(commandBlockInstance.getType()).setLore(lore).build());
        }
        allCBsCompletableFuture.complete(commandBlocks);

        return allCBsCompletableFuture;
    }
}