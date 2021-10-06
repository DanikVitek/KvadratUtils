package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class EntityManagerCommand implements CommandExecutor, Listener {
    private static final HashMap<UUID, Byte> pages = new HashMap<>();
    private static final boolean[] entity_spawn;
    private static final List<String> keys;
    public static final BukkitTask removeXPOrbsInChunksTask;

    static {
        keys = new ArrayList<>(Main.getModifyEntityManagerFile().getKeys(false));
        entity_spawn = new boolean[31];
        for (int i = 0; i < entity_spawn.length; i++)
            entity_spawn[i] = Main.getModifyEntityManagerFile().getBoolean(keys.get(i));
        removeXPOrbsInChunksTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity_spawn[23])
                    for (World world: Bukkit.getWorlds())
                        for (Chunk chunk: world.getLoadedChunks())
                            for (Entity entity: chunk.getEntities())
                                try {
                                    if (entity instanceof ExperienceOrb)
                                        entity.remove();
                                } catch (NoSuchElementException ignored) {}
            }
        }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 1L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.moderator") ||
                player.hasPermission("kvadratutils.command.entity_manager")) {
                Inventory managerInventory = Bukkit.createInventory(null, 27, "Спавн сущностей");
                Menu managerMenu = new Menu(managerInventory);
                pages.put(player.getUniqueId(), (byte) 0);
                redrawMenu(player, managerMenu, false);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private void redrawMenu(Player player, Menu managerMenu, boolean reload) {
        setIcons(managerMenu, pages.get(player.getUniqueId()));
        setToggleControls(managerMenu, pages.get(player.getUniqueId()));
        setPageControls(managerMenu, player);
        if (reload)
            Main.getMenuHandler().reloadMenu(player);
        else {
            Main.getMenuHandler().closeMenu(player);
            Main.getMenuHandler().openMenu(player, managerMenu);
        }
    }

    private void setPageControls(Menu managerMenu, Player player) {
        managerMenu.setButton(18, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(
                        player.getUniqueId(),
                        (byte) (pages.get(player.getUniqueId()) == 0
                                ? (byte) Math.ceil((double) entity_spawn.length / 9d) - 1
                                : pages.get(player.getUniqueId()) - 1));
                redrawMenu(player, managerMenu, true);
            }
        });
        managerMenu.setButton(26, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(player.getUniqueId(), (byte) ((pages.get(player.getUniqueId()) + 1) % (int) Math.ceil((double) entity_spawn.length / 9d)));
                redrawMenu(player, managerMenu, true);
            }
        });
        managerMenu.setButton(22, new Button(ControlButtons.QUIT.getItemStack()) {
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
    }

    private void setToggleControls(Menu managerMenu, byte page) {
        List<ItemStack> toggleControls = new ArrayList<>();
        switch (page) {
            case 0: {
                for (int i = 0; i < 9; i++) {
                    toggleControls.add(
                            entity_spawn[i]
                                    ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "Разрешен").build()
                                    : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "Запрещён").build()
                    );
                }
                break;
            }
            case 1: {
                for (int i = 9; i < 18; i++) {
                    toggleControls.add(
                            entity_spawn[i]
                                    ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "Разрешен").build()
                                    : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "Запрещён").build()
                    );
                }
                break;
            }
            case 2: {
                for (int i = 18; i < 27; i++) {
                    toggleControls.add(
                            entity_spawn[i]
                                    ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "Разрешен").build()
                                    : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "Запрещён").build()
                    );
                }
                break;
            }
            case 3: {
                for (int i = 27; i < entity_spawn.length; i++) {
                    toggleControls.add(
                            entity_spawn[i]
                                    ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "Разрешен").build()
                                    : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "Запрещён").build()
                    );
                }
                break;
            }
        }
        for (int i = 0; i < 9; i++) {
            final int _i = i;
            managerMenu.setButton(i + 9, new Button(i < toggleControls.size() ? toggleControls.get(i) : null) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                    entity_spawn[_i + page * 9] = !entity_spawn[_i + page * 9];
                    Main.getModifyEntityManagerFile().set(keys.get(_i + page * 9), entity_spawn[_i + page * 9]);
                    try {
                        Main.getModifyEntityManagerFile().save(Main.getEntityManagerFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    redrawMenu((Player) event.getWhoClicked(), managerMenu, true);
                }
            });
        }
        managerMenu.setButton(21, new Button(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName(ChatColor.RED + "Отключить всё").build()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Arrays.fill(entity_spawn, false);
                for (String key: keys)
                    Main.getModifyEntityManagerFile().set(key, false);
                try {
                    Main.getModifyEntityManagerFile().save(Main.getEntityManagerFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                redrawMenu((Player) event.getWhoClicked(), managerMenu, true);
            }
        });
        managerMenu.setButton(23, new Button(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName(ChatColor.GREEN + "Включить всё").build()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                Arrays.fill(entity_spawn, true);
                for (String key: keys)
                    Main.getModifyEntityManagerFile().set(key, true);
                try {
                    Main.getModifyEntityManagerFile().save(Main.getEntityManagerFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                redrawMenu((Player) event.getWhoClicked(), managerMenu, true);
            }
        });
    }

    private void setIcons(Menu managerMenu, byte page) {
        List<ItemStack> icons = new ArrayList<>();
        switch (page) {
            case 0: {
                icons.add(new ItemBuilder(Material.ARROW).setDisplayName("Стрелы").build());
                icons.add(new ItemBuilder(Material.TNT).setDisplayName("Динамит").build());
                icons.add(new ItemBuilder(Material.FIRE_CHARGE).setDisplayName("Фаерболы").build());
                icons.add(new ItemBuilder(Material.SAND).setDisplayName("Падающие блоки").build());
                icons.add(new ItemBuilder(Material.ANVIL).setDisplayName("Падающие наковальни").build());
                icons.add(new ItemBuilder(Material.TRIDENT).setDisplayName("Трезубцы").addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build());
                icons.add(new ItemBuilder(Material.FIREWORK_ROCKET).setDisplayName("Фейерверки").build());
                icons.add(new ItemBuilder(Material.CAT_SPAWN_EGG).setDisplayName("Мирные мобы").build());
                icons.add(new ItemBuilder(Material.CREEPER_SPAWN_EGG).setDisplayName("Враждебные мобы").build());
                break;
            }
            case 1: {
                icons.add(new ItemBuilder(Material.MINECART).setDisplayName("Вагонетки").build());
                icons.add(new ItemBuilder(Material.CHEST_MINECART).setDisplayName("Грузовые вагонетки").build());
                icons.add(new ItemBuilder(Material.HOPPER_MINECART).setDisplayName("Загрузочные вагонетки").build());
                icons.add(new ItemBuilder(Material.TNT_MINECART).setDisplayName("Вагонетки с динамитом").build());
                icons.add(new ItemBuilder(Material.FURNACE_MINECART).setDisplayName("Самоходные вагонетки").build());
                icons.add(new ItemBuilder(Material.COMMAND_BLOCK_MINECART).setDisplayName("Вагонетки с командным блоком").build());
                icons.add(new ItemBuilder(Material.SPAWNER).setDisplayName("Вагонетки со спавнером").build());
                icons.add(new ItemBuilder(Material.SHULKER_SPAWN_EGG).setDisplayName("Снаряды шалкера").build());
                icons.add(new ItemBuilder(Material.ITEM_FRAME).setDisplayName("Рамки и картины").build());
                break;
            }
            case 2: {
                icons.add(new ItemBuilder(Material.SNOWBALL).setDisplayName("Снежки").build());
                icons.add(new ItemBuilder(Material.ENDER_PEARL).setDisplayName("Жемчуги Края").build());
                icons.add(new ItemBuilder(Material.ENDER_EYE).setDisplayName("Глаза Края").build());
                icons.add(new ItemBuilder(Material.END_CRYSTAL).setDisplayName("Кристалы края").build());
                icons.add(new ItemBuilder(Material.DRAGON_EGG).setDisplayName("Боссы").build());
                icons.add(new ItemBuilder(Material.EXPERIENCE_BOTTLE).setDisplayName("Бутылоки и сферы опыта").build());
                icons.add(new ItemBuilder(Material.SPLASH_POTION).setDisplayName("Зелья").build());
                icons.add(new ItemBuilder(Material.PAPER).setDisplayName("Предметы").build());
                icons.add(new ItemBuilder(Material.ARMOR_STAND).setDisplayName("Стойки для брони").build());
                break;
            }
            case 3: {
                icons.add(new ItemBuilder(Material.OAK_BOAT).setDisplayName("Лодки").build());
                icons.add(new ItemBuilder(Material.LEAD).setDisplayName("Узлы поводков").build());
                icons.add(new ItemBuilder(Material.EVOKER_SPAWN_EGG).setDisplayName("Челюсти призывателя").build());
                icons.add(new ItemBuilder(Material.BLAZE_ROD).setDisplayName("Гром и молнии").build());
            }
        }
        for (int i = 0; i < 9; i++) {
            managerMenu.setButton(i, new Button(i < icons.size() ? icons.get(i) : null) {
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (
            (!entity_spawn[0] && entity instanceof AbstractArrow) ||
            (!entity_spawn[1] && entity instanceof TNTPrimed) ||
            (!entity_spawn[2] && entity instanceof Fireball) ||
            (!entity_spawn[3] && entity instanceof FallingBlock && (((FallingBlock) entity).getBlockData().getMaterial() != Material.ANVIL && ((FallingBlock) entity).getBlockData().getMaterial() != Material.CHIPPED_ANVIL && ((FallingBlock) entity).getBlockData().getMaterial() != Material.DAMAGED_ANVIL)) ||
            (!entity_spawn[4] && entity instanceof FallingBlock && (((FallingBlock) entity).getBlockData().getMaterial() == Material.ANVIL || ((FallingBlock) entity).getBlockData().getMaterial() == Material.CHIPPED_ANVIL || ((FallingBlock) entity).getBlockData().getMaterial() == Material.DAMAGED_ANVIL)) ||
            (!entity_spawn[5] && entity instanceof Trident) ||
            (!entity_spawn[6] && entity instanceof Firework) ||
            (!entity_spawn[7] && ((entity instanceof Creature && !(entity instanceof Monster)) || entity instanceof Ambient)) ||
            (!entity_spawn[8] && (entity instanceof Monster || entity instanceof Ghast || entity instanceof Slime || entity instanceof Phantom)) ||

            (!entity_spawn[16] && entity instanceof ShulkerBullet) ||
            (!entity_spawn[17] && (entity instanceof ItemFrame || entity instanceof Painting)) ||
            (!entity_spawn[18] && entity instanceof Snowball) ||
            (!entity_spawn[19] && entity instanceof EnderPearl) ||
            (!entity_spawn[20] && entity instanceof EnderSignal) ||
            (!entity_spawn[21] && entity instanceof EnderCrystal) ||
            (!entity_spawn[22] && entity instanceof Boss) ||
            (!entity_spawn[23] && entity instanceof ThrownExpBottle) ||
            (!entity_spawn[24] && entity instanceof ThrownPotion) ||
            (!entity_spawn[25] && entity instanceof Item) ||
            (!entity_spawn[26] && entity instanceof ArmorStand) ||

            (!entity_spawn[28] && entity instanceof LeashHitch) ||
            (!entity_spawn[29] && entity instanceof EvokerFangs)
        )
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicle(VehicleCreateEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (
            (!entity_spawn[9]  && vehicle instanceof RideableMinecart) ||
            (!entity_spawn[10] && vehicle instanceof StorageMinecart) ||
            (!entity_spawn[11] && vehicle instanceof HopperMinecart) ||
            (!entity_spawn[12] && vehicle instanceof ExplosiveMinecart) ||
            (!entity_spawn[13] && vehicle instanceof PoweredMinecart) ||
            (!entity_spawn[14] && vehicle instanceof CommandMinecart) ||
            (!entity_spawn[15] && vehicle instanceof SpawnerMinecart) ||

            (!entity_spawn[27] && vehicle instanceof Boat)
        )
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLightningStrike(LightningStrikeEvent event) {
        if (!entity_spawn[30])
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!entity_spawn[25])
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTargetLivingEntity(EntityTargetEvent event) {
        if (entity_spawn[23] && event.getTarget() instanceof ExperienceOrb) {
            event.getTarget().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onXPOrbEntityDrop(EntityDeathEvent event) {
        if (!entity_spawn[23])
            event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockExpEvent event) {
        if (!entity_spawn[23])
            event.setExpToDrop(0);
    }
}