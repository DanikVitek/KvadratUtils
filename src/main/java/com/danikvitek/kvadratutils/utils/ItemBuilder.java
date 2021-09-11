package com.danikvitek.kvadratutils.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.Colorable;

import java.util.Arrays;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    /**
     * The constructor method.
     * @param material the material of the item you are building.
     * @param amount   the amount of items in the item you are building.
     */
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    private void updateItemMeta() {
        this.itemStack.setItemMeta(this.itemMeta);
    }

    /**
     * Sets the display name of the item, this name can be viewed by hovering over the * item in your inventory or holding it in your hand.
     * @param name the name to set for the item.
     * @return the ItemBuilder.
     */
    public ItemBuilder setDisplayName(String name) {
        this.itemMeta.setDisplayName(name);
        return this;
    }

    /**
     * @param lines the strings to set as the item lore.
     * @return the ItemBuilder.
     */
    public ItemBuilder setLore(String... lines) {
        this.itemMeta.setLore(Arrays.asList(lines));
        return this;
    }

    /**
     * Sets the ItemStack being unbreakable
     * @param unbreakable if the result ItemStack is unbreakable
     * @return the ItemBuilder
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Sets visibility of Unbreakable tag
     * @param visible if tag is visible
     * @return the ItemBuilder
     */
    public ItemBuilder setUnbreakableTagVisible(boolean visible) {
        if (visible)
            this.itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        else
            this.itemMeta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Sets the color of the ItemStack.
     * Does nothing if the ItemStack can't be dyed
     * @param color new Color
     * @return the ItemBuilder
     */
    public ItemBuilder setColor(Color color) {
        if (itemMeta instanceof LeatherArmorMeta)
            ((LeatherArmorMeta) itemMeta).setColor(color);
        return this;
    }

    /**
     * Sets visibility of Color tag
     * @param visible if tag is visible
     * @return the ItemBuilder
     */
    public ItemBuilder setColorTagVisible(boolean visible) {
        if (visible)
            this.itemMeta.addItemFlags(ItemFlag.HIDE_DYE);
        else
            this.itemMeta.removeItemFlags(ItemFlag.HIDE_DYE);
        return this;
    }

    /**
     * @return the ItemStack that has been created.
     */
    public ItemStack build() {
        this.updateItemMeta();
        return this.itemStack;
    }
}
