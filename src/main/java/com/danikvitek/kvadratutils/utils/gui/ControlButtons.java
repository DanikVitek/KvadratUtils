package com.danikvitek.kvadratutils.utils.gui;

import com.danikvitek.kvadratutils.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ControlButtons {
    ARROW_LEFT(new ItemBuilder(Material.PLAYER_HEAD)
            .setDisplayName("Назад")
            .setOwner("MHF_ArrowLeft")
            .build()),
    ARROW_RIGHT(new ItemBuilder(Material.PLAYER_HEAD)
            .setDisplayName("Вперёд")
            .setOwner("MHF_ArrowRight")
            .build()),
    QUIT(new ItemBuilder(Material.BARRIER)
            .setDisplayName("Выйти")
            .build());

    private final ItemStack itemStack;

    private ControlButtons(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
