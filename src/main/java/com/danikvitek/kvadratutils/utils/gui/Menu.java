package com.danikvitek.kvadratutils.utils.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class Menu {
    private final Inventory inventory;
    private final HashMap<Integer, Button> buttons;

    public Menu(Inventory inventory) {
        this.inventory = inventory;
        buttons = new HashMap<>();
    }

    public void setButton(int slot, Button button) {
        buttons.put(slot, button);
    }

    public void performClick(Menu menu, InventoryClickEvent event) {
        if (buttons.get(event.getSlot()) != null)
            buttons.get(event.getSlot()).onClick(menu, event);
    }

    private void loadButtons() {
        buttons.forEach(inventory::setItem);
    }

    public void open(Player player) {
        loadButtons();
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
