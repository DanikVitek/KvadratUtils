package com.danikvitek.kvadratutils.utils.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class Menu {
    private final Inventory inventory;
    private final HashMap<Integer, Button> buttons = new HashMap<>();

    public Menu(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setButton(int slot, Button button) {
        buttons.put(slot, button);
    }

    public void performClick(InventoryClickEvent event) {
        if (buttons.get(event.getSlot()) != null)
            buttons.get(event.getSlot()).onClick(this, event);
    }

    protected void loadButtons() {
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