package com.danikvitek.kvadratutils.utils.gui;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PageUtil {
    public static int getMaxPages(List<ItemStack> allItems, int spaces) {
        return (int) Math.ceil((double) allItems.size() / (double) spaces);
    }

    public static List<ItemStack> getPageItems(List<ItemStack> allItems, int page, int spaces) {
        return new ArrayList<>(allItems.subList(spaces * page , Math.min(spaces * (page + 1), allItems.size())));
    }
}
