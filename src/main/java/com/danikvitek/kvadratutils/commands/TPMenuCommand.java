package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.api.events.PlayerTeleportMenuEvent;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TPMenuCommand implements CommandExecutor, Listener {
    private static final HashMap<UUID, Integer> pages = new HashMap<>();
    private static final HashMap<UUID, Long> collisionCooldowns = new HashMap<>();
    private static final Team notCollidableTeam;
    private static final long cooldown = 10000L;

    static {
        notCollidableTeam = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard().registerNewTeam("not_collidable");
        notCollidableTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        notCollidableTeam.addEntry(ChatColor.BLACK + "" + ChatColor.WHITE);
    }

    public static final BukkitTask collisionCooldownTask = new BukkitRunnable() {
        @Override
        public void run() {
            try {
                for (Map.Entry<UUID, Long> entry : collisionCooldowns.entrySet()) {
                    if (System.currentTimeMillis() > entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
//                            System.out.println(notCollidableTeam.hasPlayer(player));
                            notCollidableTeam.removePlayer(player);
                        }
                        collisionCooldowns.remove(entry.getKey());
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                e.printStackTrace();
            } catch (ConcurrentModificationException ignored) {}
        }
    }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 1L);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.command.tp_menu")) {
                Inventory teleportInventory = Bukkit.createInventory(null, 36, "????????????????????????");
                Menu teleportMenu = new Menu(teleportInventory);
                pages.put(player.getUniqueId(), 0);
                redrawMenu(player, teleportMenu, false);
            }
            else
                player.sendMessage(ChatColor.RED + "?????? ???????? ???? ?????????????????????????? ??????????????");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "?????????????? ?????????? ???????????????????????? ???????????? ??????????");
        return true;
    }

    private static void redrawMenu(Player player, Menu teleportMenu, boolean reload) {
        List<ItemStack> playerHeads = getPlayerHeads(player);
        setPageControls(player, teleportMenu, playerHeads);
        setTeleportControls(player, teleportMenu, playerHeads);
        setPlayerHeads(player, teleportMenu, playerHeads);
        if (reload)
            Main.getMenuHandler().reloadMenu(player);
        else {
            Main.getMenuHandler().closeMenu(player);
            Main.getMenuHandler().openMenu(player, teleportMenu);
        }
    }

    private static void setPageControls(Player player, Menu teleportMenu, List<ItemStack> playerHeads) {
        teleportMenu.setButton(27, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), pages.get(player.getUniqueId()) <= 0 ? PageUtil.getMaxPages(playerHeads, 9) - 1 : pages.get(player.getUniqueId()) - 1);
                redrawMenu(player, teleportMenu, true);
            }
        });
        teleportMenu.setButton(31, new Button(ControlButtons.QUIT.getItemStack()) {
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
        teleportMenu.setButton(35, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), (pages.get(player.getUniqueId()) + 1) % PageUtil.getMaxPages(playerHeads, 9));
                redrawMenu(player, teleportMenu, true);
            }
        });
    }

    private static void setPlayerHeads(Player player, Menu teleportMenu, List<ItemStack> playerHeads) {
        List<ItemStack> pageHeads = PageUtil.getPageItems(playerHeads, pages.get(player.getUniqueId()), 9);
        for (int i = 0; i < 9; i++) {
            teleportMenu.setButton(i, new Button(i < pageHeads.size() ? pageHeads.get(i) : null) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                }
            });
        }
    }

    private static void setTeleportControls(Player player, Menu teleportMenu, List<ItemStack> playerHeads) {
        List<ItemStack> pageHeads = PageUtil.getPageItems(playerHeads, pages.get(player.getUniqueId()), 9);
        for (int i = 0; i < 9; i++) {
            Player subjectPlayer = i < pageHeads.size() ? Bukkit.getPlayer(Objects.requireNonNull(pageHeads.get(i).getItemMeta()).getDisplayName()) : null;
            teleportMenu.setButton(i + 9, new Button(i < pageHeads.size()
                    ? new ItemBuilder(Objects.requireNonNull(subjectPlayer).hasPermission("kvadratutils.can_teleport_to") && player.hasPermission("kvadratutils.teleport_to_player." + subjectPlayer.getName())
                            ? Material.CYAN_WOOL
                            : Material.LIGHT_GRAY_WOOL).setDisplayName(ChatColor.AQUA + "?????????????????????????????????? ?? " + subjectPlayer.getName()).build()
                    : null) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                    if (subjectPlayer != null && subjectPlayer.hasPermission("kvadratutils.can_teleport_to") && player.hasPermission("kvadratutils.teleport_to_player." + subjectPlayer.getName())) {
                        PlayerTeleportMenuEvent playerTeleportMenuEvent = new PlayerTeleportMenuEvent(player, subjectPlayer, false, PlayerTeleportEvent.TeleportCause.PLUGIN);
                        Bukkit.getPluginManager().callEvent(playerTeleportMenuEvent);
                        if (!playerTeleportMenuEvent.isCancelled()) {
                            notCollidableTeam.addPlayer(player);
                            collisionCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown);
                            notCollidableTeam.addPlayer(subjectPlayer);
                            collisionCooldowns.put(subjectPlayer.getUniqueId(), System.currentTimeMillis() + cooldown);
                            player.teleport(subjectPlayer, PlayerTeleportEvent.TeleportCause.PLUGIN);
                            Main.getMenuHandler().closeMenu(player);
                        }
                    }
                }
            });
            teleportMenu.setButton(i + 18, new Button(i < pageHeads.size()
                    ? new ItemBuilder(Objects.requireNonNull(subjectPlayer).hasPermission("kvadratutils.can_teleport") && player.hasPermission("kvadratutils.teleport_player." + subjectPlayer.getName())
                            ? Material.BLUE_WOOL
                            : Material.GRAY_WOOL).setDisplayName(ChatColor.DARK_AQUA + "?????????????????????????????? " + subjectPlayer.getName() + " ?? ????????").build()
                    : null) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                    if (subjectPlayer != null && subjectPlayer.hasPermission("kvadratutils.can_teleport") && player.hasPermission("kvadratutils.teleport_player." + subjectPlayer.getName())) {
                        PlayerTeleportMenuEvent playerTeleportMenuEvent = new PlayerTeleportMenuEvent(subjectPlayer, player, true, PlayerTeleportEvent.TeleportCause.PLUGIN);
                        Bukkit.getPluginManager().callEvent(playerTeleportMenuEvent);
                        if (!playerTeleportMenuEvent.isCancelled()) {
                            player.setCollidable(false);
                            collisionCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown);
                            subjectPlayer.setCollidable(false);
                            collisionCooldowns.put(subjectPlayer.getUniqueId(), System.currentTimeMillis() + cooldown);
                            subjectPlayer.teleport(player, PlayerTeleportEvent.TeleportCause.PLUGIN);
                        }
                    }
                }
            });
        }
        teleportMenu.setButton(30, new Button(new ItemBuilder(player.hasPermission("kvadratutils.can_teleport_to")
                ? Material.CYAN_STAINED_GLASS_PANE
                : Material.GRAY_STAINED_GLASS_PANE).setDisplayName(ChatColor.AQUA + "???????????????????? ?????????????????? ?? ????????").build()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Main.getLuckPermsAPI().getUserManager().modifyUser(player.getUniqueId(), u -> u.data().add(PermissionNode.builder("kvadratutils.can_teleport_to").value(!player.hasPermission("kvadratutils.can_teleport_to")).build()));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        redrawMenu(player, teleportMenu, true);
                    }
                }.runTaskLater(Main.getPlugin(Main.class), 2L);
            }
        });
        teleportMenu.setButton(32, new Button(new ItemBuilder(player.hasPermission("kvadratutils.can_teleport")
                ? Material.BLUE_STAINED_GLASS_PANE
                : Material.BLACK_STAINED_GLASS_PANE).setDisplayName(ChatColor.DARK_AQUA + "???????????????????? ?????????????????? ???????? ?? ????????????").build()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Main.getLuckPermsAPI().getUserManager().modifyUser(player.getUniqueId(), u -> u.data().add(PermissionNode.builder("kvadratutils.can_teleport").value(!player.hasPermission("kvadratutils.can_teleport")).build()));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        redrawMenu(player, teleportMenu, true);
                    }
                }.runTaskLater(Main.getPlugin(Main.class), 2L);
            }
        });
    }

    private static List<ItemStack> getPlayerHeads(Player player) {
        return Bukkit.getOnlinePlayers().stream().filter(p -> !p.equals(player))
                .map(p -> new ItemBuilder(Material.PLAYER_HEAD).setDisplayName(p.getName()).setOwner(p.getName()).build())
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onMenuTeleport(PlayerTeleportMenuEvent event) {
        if (event.isForced()) { // destination teleports target to itself
            Player target = event.getTarget(),
                   destination = event.getDestination();
            if (!target.hasPermission("kvadratutils.can_teleport") || !destination.hasPermission("kvadratutils.teleport_player." + target.getName()))
                event.setCancelled(true);
        }
        else { // target teleports itself to destination
            Player target = event.getTarget(),
                   destination = event.getDestination();
            if (!destination.hasPermission("kvadratutils.can_teleport_to") || !target.hasPermission("kvadratutils.teleport_to_player." + destination.getName()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTPCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        List<String> words = Arrays.asList(command.split(" "));
        if (words.get(0).equals("/teleport") || words.get(0).equals("/tp")) {
            List<String> args = words.subList(1, words.size());
            if (args.size() == 1) { // target teleports itself to destination
                Player target = event.getPlayer(),
                       destination = Bukkit.getPlayer(args.get(0));
                if (destination != null && (!destination.hasPermission("kvadratutils.can_teleport_to") || !target.hasPermission("kvadratutils.teleport_to_player." + destination.getName())))
                    event.setCancelled(true);
            }
            else if (args.size() == 2) {
                Player target = Bukkit.getPlayer(args.get(0)),
                       destination = Bukkit.getPlayer(args.get(1));
                if (target != null && destination != null) {
                    if (Objects.equals(event.getPlayer(), target)) { // target teleports itself to destination
                        if (!destination.hasPermission("kvadratutils.can_teleport_to") || !target.hasPermission("kvadratutils.teleport_to_player." + destination.getName()))
                            event.setCancelled(true);
                    }
                    else if (Objects.equals(event.getPlayer(), destination)) { // destination teleports target to itself
                        if (!target.hasPermission("kvadratutils.can_teleport") || !destination.hasPermission("kvadratutils.teleport_player." + target.getName()))
                            event.setCancelled(true);
                    }
                }
            }
        }
    }
}