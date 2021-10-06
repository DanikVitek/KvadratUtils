package com.danikvitek.kvadratutils.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private ItemStack itemStack;
    private ItemMeta itemMeta;

    /**
     * Material-based constructor
     * @param material the material of the item you are building.
     * @param amount   the amount of items in the item you are building.
     */
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * Material-based constructor
     * @param material the material of the item you are building.
     */
    public ItemBuilder(Material material) {
        this(material, 1);
    }

    /**
     * ItemStack-based constructor
     * @param itemStack old ItemStack
     * @param amount    new amount of items in the ItemStack
     */
    public ItemBuilder(ItemStack itemStack, int amount) {
        this.itemStack = itemStack;
        this.itemStack.setAmount(amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * ItemStack-based constructor
     * @param itemStack old ItemStack
     */
    public ItemBuilder(ItemStack itemStack) {
        this(itemStack, itemStack.getAmount());
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
     * @param lore the strings list to set as the item lore.
     * @return the ItemBuilder.
     */
    public ItemBuilder setLore(List<String> lore) {
        this.itemMeta.setLore(lore);
        return this;
    }

    /**
     * @param line new lone at the lore
     * @return the ItemBuilder
     */
    public ItemBuilder addLore(String line) {
        List<String> lore = this.itemMeta.getLore();
        assert lore != null;
        lore.add(line);
        this.itemMeta.setLore(lore);
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
     * Set itemflags which should be ignored when rendering a ItemStack in the Client. This Method does silently ignore double set itemFlags.
     * @param itemFlags set of ItemFlags
     * @return the ItemBuilder
     */
    public ItemBuilder addItemFlags(ItemFlag ...itemFlags) {
        this.itemMeta.addItemFlags(itemFlags);
        return this;
    }

    /**
     * Remove specific set of itemFlags. This tells the Client it should render it again. This Method does silently ignore double removed itemFlags.
     * @param itemFlags set of ItemFlags
     * @return the ItemBuilder
     */
    public ItemBuilder removeItemFlags(ItemFlag ...itemFlags) {
        this.itemMeta.removeItemFlags(itemFlags);
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
     * Sets the owner of the skull
     * Does nothing if the ItemStack is not a skull
     * @param owner skull owner
     * @return the ItemBuilder
     */
    public ItemBuilder setOwner(String owner) {
        if (this.itemMeta instanceof SkullMeta)
            ((SkullMeta) this.itemMeta).setOwner(owner);
        return this;
    }

    /**
     * @return the ItemStack that has been created.
     */
    public ItemStack build() {
        updateItemMeta();
        return this.itemStack;
    }

    /**
     * @return the ItemMeta
     */
    public ItemMeta getItemMeta() {
        return itemMeta;
    }
}