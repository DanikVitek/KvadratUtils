package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import com.danikvitek.kvadratutils.utils.gui.PageUtil;
import com.danikvitek.kvadratutils.utils.nms.MinecraftVersion;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GameRulesCommand implements CommandExecutor {
    private static final HashMap<UUID, Byte> pages;
    private static final ArrayList<ItemStack> gameRulesIcons;

    static {
        pages = new HashMap<>();
        gameRulesIcons = new ArrayList<>();
        gameRulesIcons.addAll(Arrays.asList(
                new ItemBuilder(Material.NETHER_STAR)
                        .setDisplayName(ChatColor.GOLD + "announceAdvancements")
                        .setLore(ChatColor.YELLOW + "Переключает отображение",
                                ChatColor.YELLOW + "достижений в чате").build(),
                new ItemBuilder(Material.COMMAND_BLOCK)
                        .setDisplayName(ChatColor.GOLD + "commandBlockOutput")
                        .setLore(ChatColor.YELLOW + "Должны ли командные блоки",
                                ChatColor.YELLOW + "оповещать операторов когда они",
                                ChatColor.YELLOW + "исполняют команду").build(),
                new ItemBuilder(Material.ELYTRA)
                        .setDisplayName(ChatColor.GOLD + "disableElytraMovementCheck")
                        .setLore(ChatColor.YELLOW + "Должен ли сервер пропустить",
                                ChatColor.YELLOW + "проверку скорости игрока,",
                                ChatColor.YELLOW + "когда он носит элитры").build(),
                new ItemBuilder(Material.CLOCK)
                        .setDisplayName(ChatColor.GOLD + "doDaylightCycle")
                        .setLore(ChatColor.YELLOW + "Меняется ли время суток в мире").build(),
                new ItemBuilder(Material.PAINTING)
                        .setDisplayName(ChatColor.GOLD + "doEntityDrops")
                        .setLore(ChatColor.YELLOW + "Должны ли сущности, которые",
                                ChatColor.YELLOW + "не являются мобами, дропать предметы").build(),
                new ItemBuilder(Material.FLINT_AND_STEEL)
                        .setDisplayName(ChatColor.GOLD + "doFireTick")
                        .setLore(ChatColor.YELLOW + "Должен ли огонь распространяться",
                                ChatColor.YELLOW + "и тухнуть естественным образом").build(),
                new ItemBuilder(Material.KNOWLEDGE_BOOK)
                        .setDisplayName(ChatColor.GOLD + "doLimitedCrafting")
                        .setLore(ChatColor.YELLOW + "Должны ли игроки быть способны",
                                ChatColor.YELLOW + "крафтить предметы только имея",
                                ChatColor.YELLOW + "разблокированные рецепты").build(),
                new ItemBuilder(Material.PORKCHOP)
                        .setDisplayName(ChatColor.GOLD + "doMobLoot")
                        .setLore(ChatColor.YELLOW + "Должны ли мобы дропать предметы").build(),
                new ItemBuilder(Material.SPAWNER)
                        .setDisplayName(ChatColor.GOLD + "doMobSpawning")
                        .setLore(ChatColor.YELLOW + "Должны ли мобы спавниться",
                                ChatColor.YELLOW + "естественным образом").build(),
                new ItemBuilder(Material.CHEST)
                        .setDisplayName(ChatColor.GOLD + "doTileDrops")
                        .setLore(ChatColor.YELLOW + "Должны ли блоки дропать предметы").build(),
                new ItemBuilder(Material.CAULDRON)
                        .setDisplayName(ChatColor.GOLD + "doWeatherCycle")
                        .setLore(ChatColor.YELLOW + "Должна ли погода меняться").build(),
                new ItemBuilder(Material.YELLOW_SHULKER_BOX)
                        .setDisplayName(ChatColor.GOLD + "keepInventory")
                        .setLore(ChatColor.YELLOW + "Сохраняется ли инвентарь",
                                ChatColor.YELLOW + "игроков после смерти").build(),
                new ItemBuilder(Material.COMMAND_BLOCK_MINECART)
                        .setDisplayName(ChatColor.GOLD + "logAdminCommands")
                        .setLore(ChatColor.YELLOW + "Записывать ли команды",
                                ChatColor.YELLOW + "оператора в логи сервера").build(),
                new ItemBuilder(Material.CREEPER_SPAWN_EGG)
                        .setDisplayName(ChatColor.GOLD + "mobGriefing")
                        .setLore(ChatColor.YELLOW + "Могут ли мобы подбирать",
                                ChatColor.YELLOW + "предметы или менять блоки").build(),
                new ItemBuilder(Material.GOLDEN_CARROT)
                        .setDisplayName(ChatColor.GOLD + "naturalRegeneration")
                        .setLore(ChatColor.YELLOW + "Могут ли игроки регенерировать",
                                ChatColor.YELLOW + "здоровье естественным образом",
                                ChatColor.YELLOW + "с помощью шкалы сытости").build(),
                new ItemBuilder(Material.OAK_SIGN)
                        .setDisplayName(ChatColor.GOLD + "reducedDebugInfo")
                        .setLore(ChatColor.YELLOW + "Показывает ли экран отладки",
                                ChatColor.YELLOW + "полную или сокращённую информацию").build(),
                new ItemBuilder(Material.WRITABLE_BOOK)
                        .setDisplayName(ChatColor.GOLD + "sendCommandFeedback")
                        .setLore(ChatColor.YELLOW + "Должен ли отклик от команд,",
                                ChatColor.YELLOW + "исполненных игроком, отображаться",
                                ChatColor.YELLOW + "в чате. Это также влияет на то,",
                                ChatColor.YELLOW + "сохраняют ли командные блоки",
                                ChatColor.YELLOW + "свой выводдной текст").build(),
                new ItemBuilder(Material.SKELETON_SKULL)
                        .setDisplayName(ChatColor.GOLD + "showDeathMessages")
                        .setLore(ChatColor.YELLOW + "Появляется ли сообщение о смерти",
                                ChatColor.YELLOW + "в чате после гибели игрока").build(),
                new ItemBuilder(Material.ENDER_EYE)
                        .setDisplayName(ChatColor.GOLD + "spectatorsGenerateChunks")
                        .setLore(ChatColor.YELLOW + "Могут ли наблюдатели генерировать",
                                ChatColor.YELLOW + "новые чанки").build(),
                new ItemBuilder(Material.IRON_AXE)
                        .setDisplayName(ChatColor.GOLD + "disableRaids")
                        .setLore(ChatColor.YELLOW + "Должны ли рейды быть отключены",
                                ChatColor.YELLOW + "или нет").build(),
                new ItemBuilder(Material.PHANTOM_SPAWN_EGG)
                        .setDisplayName(ChatColor.GOLD + "doInsomnia")
                        .setLore(ChatColor.YELLOW + "Должны ли фантомы появляться",
                                ChatColor.YELLOW + "при отсутствии сна").build(),
                new ItemBuilder(Material.RED_BED)
                        .setDisplayName(ChatColor.GOLD + "doImmediateRespawn")
                        .setLore(ChatColor.YELLOW + "Должны ли игроки моментально",
                                ChatColor.YELLOW + "возраждаться после смерти").build(),
                new ItemBuilder(Material.TURTLE_HELMET)
                        .setDisplayName(ChatColor.GOLD + "drowningDamage")
                        .setLore(ChatColor.YELLOW + "Должно ли утопление наносить урон").build(),
                new ItemBuilder(Material.FEATHER)
                        .setDisplayName(ChatColor.GOLD + "fallDamage")
                        .setLore(ChatColor.YELLOW + "Должен ли быть урон от падения").build(),
                new ItemBuilder(Material.FIRE_CHARGE)
                        .setDisplayName(ChatColor.GOLD + "fireDamage")
                        .setLore(ChatColor.YELLOW + "Должен ли быть урон от огня").build(),
                new ItemBuilder(Material.VINDICATOR_SPAWN_EGG)
                        .setDisplayName(ChatColor.GOLD + "doPatrolSpawning")
                        .setLore(ChatColor.YELLOW + "Должны ли патрли разбойников",
                                ChatColor.YELLOW + "спавниться естественным образом").build(),
                new ItemBuilder(Material.WANDERING_TRADER_SPAWN_EGG)
                        .setDisplayName(ChatColor.GOLD + "doTraderSpawning")
                        .setLore(ChatColor.YELLOW + "Должен ли странствующий торговец",
                                ChatColor.YELLOW + "спавниться естественным образом").build(),
                new ItemBuilder(Material.GHAST_TEAR)
                        .setDisplayName(ChatColor.GOLD + "forgiveDeadPlayers")
                        .setLore(ChatColor.YELLOW + "Должны ли мобы прекращать",
                                ChatColor.YELLOW + "агр после смерти игрока").build(),
                new ItemBuilder(Material.GOLDEN_SWORD)
                        .setDisplayName(ChatColor.GOLD + "universalAnger")
                        .setLore(ChatColor.YELLOW + "Должны ли мобы преследовать всех",
                                ChatColor.YELLOW + "игроков, кога они заагрены").build(),
                new ItemBuilder(Material.WHEAT_SEEDS)
                        .setDisplayName(ChatColor.GOLD + "randomTickSpeed")
                        .setLore(ChatColor.YELLOW + "Как часто должны происходить",
                                ChatColor.YELLOW + "случайные тики блоков (такие",
                                ChatColor.YELLOW + "как рост растений, опадание",
                                ChatColor.YELLOW + "листвы, и т.д.) за секцию чанков за",
                                ChatColor.YELLOW + "игровой тик.",
                                ChatColor.YELLOW + "Значение 0 отключает случайные тики,",
                                ChatColor.YELLOW + "значения выше увеличат случайные тики.").build(),
                new ItemBuilder(Material.BEACON)
                        .setDisplayName(ChatColor.GOLD + "spawnRadius")
                        .setLore(ChatColor.YELLOW + "Радиус, в котором игроки будут",
                                ChatColor.YELLOW + "случайным образом появляться вокруг",
                                ChatColor.YELLOW + "мирового центра савна при первом",
                                ChatColor.YELLOW + "заходе в игру или при смерти без",
                                ChatColor.YELLOW + "установленной точки возраждения").build(),
                new ItemBuilder(Material.CHICKEN)
                        .setDisplayName(ChatColor.GOLD + "maxEntityCramming")
                        .setLore(ChatColor.YELLOW + "Максимальное количество других",
                                ChatColor.YELLOW + "толкаемых сущностей, которые игрок",
                                ChatColor.YELLOW + "или другие мобы могут толкать до тех",
                                ChatColor.YELLOW + "пор, пока не начнут получать урон.",
                                ChatColor.YELLOW + "Значение 0 отключит этот предел").build(),
                new ItemBuilder(Material.CHAIN)
                        .setDisplayName(ChatColor.GOLD + "maxCommandChainLength")
                        .setLore(ChatColor.YELLOW + "Определяет максимальный размер",
                                ChatColor.YELLOW + "цепи командных блоков, которое",
                                ChatColor.YELLOW + "будет восприниматься как \"цепь\".",
                                ChatColor.YELLOW + "Это максимальное количество блоков,",
                                ChatColor.YELLOW + "которые будут выполнены в один тик",
                                ChatColor.YELLOW + "в одной цепи.").build()
        ));
        if (MinecraftVersion.VERSION.equals(MinecraftVersion.VersionEnum.v1_17_R1))
            gameRulesIcons.addAll(Arrays.asList(
                    new ItemBuilder(Material.valueOf("POWDER_SNOW_BUCKET"))
                            .setDisplayName(ChatColor.GOLD + "freezeDamage")
                            .setLore(ChatColor.YELLOW + "Включён ли урон от обморожения").build(),
                    new ItemBuilder(Material.DAYLIGHT_DETECTOR)
                            .setDisplayName(ChatColor.GOLD + "playersSleepingPercentage")
                            .setLore(ChatColor.YELLOW + "Процент онлайн игроков, которые",
                                    ChatColor.YELLOW + "должны спать для пропуска ночи").build()
            ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Inventory gameRulesInventory = Bukkit.createInventory(null, 27, "Игровые правила");
            Menu gameRulesMenu = new Menu(gameRulesInventory);
            pages.put(player.getUniqueId(), (byte) 0);

            redrawMenu(player, gameRulesMenu, false);
        }
        return true;
    }

    private static void redrawMenu(Player player, Menu gameRulesMenu, boolean reload) {
        List<ItemStack> pageIcons = PageUtil.getPageItems(gameRulesIcons, pages.get(player.getUniqueId()), 9);
        setPageControls(player, gameRulesMenu);
        setGameRulesIcons(player, gameRulesMenu, pageIcons);
        setGameRulesControls(player, gameRulesMenu, pageIcons);
        if (reload)
            Main.getMenuHandler().reloadMenu(player);
        else {
            Main.getMenuHandler().closeMenu(player);
            Main.getMenuHandler().openMenu(player, gameRulesMenu);
        }
    }

    private static void setPageControls(Player player, Menu gameRulesMenu) {
        gameRulesMenu.setButton(18, new Button(ControlButtons.ARROW_LEFT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(
                        player.getUniqueId(),
                        (byte) (pages.get(player.getUniqueId()) <= 0
                                ? PageUtil.getMaxPages(gameRulesIcons, 9) - 1
                                : pages.get(player.getUniqueId()) - 1));
                redrawMenu(player, gameRulesMenu, true);
            }
        });
        gameRulesMenu.setButton(26, new Button(ControlButtons.ARROW_RIGHT.getItemStack()) {
            @Override
            public void onClick(Menu menu, InventoryClickEvent event) {
                event.setCancelled(true);
                pages.put(
                        player.getUniqueId(),
                        (byte) ((pages.get(player.getUniqueId()) + 1) % PageUtil.getMaxPages(gameRulesIcons, 9)));
                redrawMenu(player, gameRulesMenu, true);
            }
        });
        gameRulesMenu.setButton(22, new Button(ControlButtons.QUIT.getItemStack()) {
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

    private static void setGameRulesIcons(Player player, Menu gameRulesMenu, List<ItemStack> pageIcons) {
        for (int i = 0; i < 9; i++) {
            gameRulesMenu.setButton(i,
                    new Button(i < pageIcons.size()
                            ? new ItemBuilder(pageIcons.get(i).clone())
                            .addLore(ChatColor.GOLD + "По умолчанию: " + player.getWorld().getGameRuleDefault(Objects.requireNonNull(GameRule.getByName(ChatColor.stripColor(Objects.requireNonNull(pageIcons.get(i).getItemMeta()).getDisplayName())))))
                            .build()
                            : null) {
                        @Override
                        public void onClick(Menu menu, InventoryClickEvent event) {
                            event.setCancelled(true);
                            redrawMenu(player, gameRulesMenu, true);
                        }
                    });
        }
    }

    private static void setGameRulesControls(Player player, Menu gameRulesMenu, List<ItemStack> pageIcons) {
        for (int i = 0; i < 9; i++) {
            GameRule<?> gameRule = i < pageIcons.size() ? GameRule.getByName(ChatColor.stripColor(Objects.requireNonNull(pageIcons.get(i).getItemMeta()).getDisplayName())) : null;
            gameRulesMenu.setButton(i + 9, new Button(
                    i < pageIcons.size()
                            ? (gameRule != null
                            ? (gameRule.getType().equals(Boolean.class)
                            ? ((boolean) player.getWorld().getGameRuleValue(gameRule)
                            ? new ItemBuilder(Material.LIME_WOOL).setDisplayName(ChatColor.GREEN + "True").build()
                            : new ItemBuilder(Material.RED_WOOL).setDisplayName(ChatColor.RED + "False").build())
                            : new ItemBuilder(Material.PAPER).setDisplayName(ChatColor.GOLD + Objects.requireNonNull(player.getWorld().getGameRuleValue(gameRule)).toString()).build())
                            : null)
                            : null) {
                @SuppressWarnings("unchecked cast")
                @Override
                public void onClick(Menu menu, InventoryClickEvent event) {
                    event.setCancelled(true);
                    if (gameRule != null) {
                        if (this.getType() != Material.PAPER && gameRule.getType().equals(Boolean.class)) {
                            boolean value = (boolean) player.getWorld().getGameRuleValue(gameRule);
                            player.getWorld().setGameRule((GameRule<Boolean>) gameRule, !value);
                            redrawMenu(player, gameRulesMenu, true);
                        } else {
                            new AnvilGUI.Builder()
                                    .itemLeft(
                                            new ItemBuilder(Material.PAPER)
                                                    .setDisplayName(Objects.requireNonNull(player.getWorld().getGameRuleValue(gameRule)).toString())
                                                    .setLore(
                                                            ChatColor.YELLOW + "По умолчанию: " + player.getWorld().getGameRuleDefault(gameRule),
                                                            ChatColor.RED + "[0-9]")
                                                    .build())
                                    .text("Новое значение")
                                    .title("Установить значение правила")
                                    .onComplete((p, t) -> {
                                        if (t.matches("[0-9]+")) {
                                            int value;
                                            try {
                                                value = Integer.parseInt(t);
                                            } catch (NumberFormatException e) {
                                                return AnvilGUI.Response.text("Не больше " + Integer.MAX_VALUE + "!");
                                            }
                                            p.getWorld().setGameRule((GameRule<Integer>) gameRule, value);
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    redrawMenu(player, gameRulesMenu, false);
                                                }
                                            }.runTaskLater(Main.getPlugin(Main.class), 3L);
                                            return AnvilGUI.Response.close();
                                        } else
                                            return AnvilGUI.Response.text("Неверный формат значения!");
                                    })
                                    .onClose(p -> new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            redrawMenu(player, gameRulesMenu, false);
                                        }
                                    }.runTaskLater(Main.getPlugin(Main.class), 3L))
                                    .plugin(Main.getPlugin(Main.class))
                                    .open(player);
                        }
                    }
                }
            });
        }
    }
}
