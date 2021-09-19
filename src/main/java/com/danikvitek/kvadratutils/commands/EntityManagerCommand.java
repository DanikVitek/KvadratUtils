package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class EntityManagerCommand implements CommandExecutor, Listener {
    private static final HashMap<UUID, Byte> pages = new HashMap<>();
    private final boolean[] entity_spawn = new boolean[26];
    private final List<String> keys;

    public EntityManagerCommand() {
        keys = new ArrayList<>(Main.getModifyEntityManagerFile().getKeys(false));
        for (int i = 0; i < entity_spawn.length; i++)
            entity_spawn[i] = Main.getModifyEntityManagerFile().getBoolean(keys.get(i));
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
                for (int i = 18; i < entity_spawn.length; i++) {
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
                icons.add(new ItemBuilder(Material.ITEM_FRAME).setDisplayName("Рамки").build());
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
                break;
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

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (
            (!entity_spawn[0] && entity instanceof Arrow) ||
            (!entity_spawn[1] && entity instanceof TNTPrimed) ||
            (!entity_spawn[2] && entity instanceof Fireball) ||
            (!entity_spawn[3] && entity instanceof FallingBlock && (((FallingBlock) entity).getBlockData().getMaterial() != Material.ANVIL && ((FallingBlock) entity).getBlockData().getMaterial() != Material.CHIPPED_ANVIL && ((FallingBlock) entity).getBlockData().getMaterial() != Material.DAMAGED_ANVIL)) ||
            (!entity_spawn[4] && entity instanceof FallingBlock && (((FallingBlock) entity).getBlockData().getMaterial() == Material.ANVIL || ((FallingBlock) entity).getBlockData().getMaterial() == Material.CHIPPED_ANVIL || ((FallingBlock) entity).getBlockData().getMaterial() == Material.DAMAGED_ANVIL)) ||
            (!entity_spawn[5] && entity instanceof Trident) ||
            (!entity_spawn[6] && entity instanceof Firework) ||
            (!entity_spawn[7] && ((entity instanceof Creature && !(entity instanceof Monster)) || entity instanceof Ambient)) ||
            (!entity_spawn[8] && entity instanceof Monster) ||
            (!entity_spawn[9] && entity instanceof RideableMinecart) || // !(entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof HopperMinecart || entity instanceof ExplosiveMinecart || entity instanceof CommandMinecart || entity instanceof SpawnerMinecart)
            (!entity_spawn[10] && entity instanceof StorageMinecart) ||
            (!entity_spawn[11] && entity instanceof HopperMinecart) ||
            (!entity_spawn[12] && entity instanceof ExplosiveMinecart) ||
            (!entity_spawn[13] && entity instanceof PoweredMinecart) ||
            (!entity_spawn[14] && entity instanceof CommandMinecart) ||
            (!entity_spawn[15] && entity instanceof SpawnerMinecart) ||
            (!entity_spawn[16] && entity instanceof ShulkerBullet) ||
            (!entity_spawn[17] && entity instanceof ItemFrame) ||
            (!entity_spawn[18] && entity instanceof Snowball) ||
            (!entity_spawn[19] && entity instanceof EnderPearl) ||
            (!entity_spawn[20] && entity instanceof EnderSignal) ||
            (!entity_spawn[21] && entity instanceof EnderCrystal) ||
            (!entity_spawn[22] && entity instanceof Boss) ||
            (!entity_spawn[23] && (entity instanceof ExperienceOrb || entity instanceof ThrownExpBottle)) ||
            (!entity_spawn[24] && entity instanceof ThrownPotion) ||
            (!entity_spawn[25] && entity instanceof Item)
        )
            event.setCancelled(true);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!entity_spawn[25])
            event.setCancelled(true);
    }
}
