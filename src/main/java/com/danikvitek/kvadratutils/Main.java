package com.danikvitek.kvadratutils;

import com.danikvitek.kvadratutils.commands.CommandBlocksCommand;
import com.danikvitek.kvadratutils.utils.gui.MenuHandler;
import com.danikvitek.kvadratutils.utils.nms.Reflector;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_17;
import com.danikvitek.kvadratutils.utils.nms.Reflector_1_8;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {

    private static MenuHandler menuHandler;
    private Reflector reflector;

    @Override
    public void onEnable() {
        menuHandler = new MenuHandler();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(menuHandler.getListeners(), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockLogger(), this);

        getCommand("command_blocks").setExecutor(new CommandBlocksCommand());

        // Multi-version
        try {
            reflector = new Reflector_1_8();
        } catch (ClassNotFoundException e1) {
            try {
                reflector = new Reflector_1_17();
            } catch (ClassNotFoundException e2) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Не удалось загрузить плагин для этой версии (" + Bukkit.getVersion() + ")");
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("kvadratutils.f3n_f3f4"))
            reflector.sendPseudoOPStatus(player);
    }

    @Override
    public void onDisable() {
        menuHandler.closeAll();
    }

    public static MenuHandler getMenuHandler() {
        return menuHandler;
    }
}
