package com.example.rshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RShop extends JavaPlugin implements Listener, TabExecutor {

    private Economy economy;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("rshop")).setExecutor(this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            openMainMenu(player);
        }
        return true;
    }

    private void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§8RShop");
        menu.setItem(10, createItem(Material.REDSTONE, "§cRedstone"));
        menu.setItem(12, createItem(Material.PISTON, "§aPiston"));
        menu.setItem(14, createItem(Material.OBSERVER, "§6Observer"));
        menu.setItem(16, createItem(Material.HOPPER, "§fHopper"));
        player.openInventory(menu);
    }

    private void openQuantityMenu(Player player, String itemName) {
        Inventory menu = Bukkit.createInventory(null, 27, "§8Select Quantity");

        int amount = 1;
        int unitPrice = getItemPrice(itemName);

        menu.setItem(11, createItem(Material.RED_STAINED_GLASS_PANE, "§c-8"));
        menu.setItem(12, createItem(Material.RED_STAINED_GLASS_PANE, "§c-1"));

        ItemStack itemStack = new ItemStack(getMaterial(itemName), amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + capitalize(itemName));
            itemStack.setItemMeta(meta);
        }
        menu.setItem(13, itemStack);

        menu.setItem(14, createItem(Material.LIME_STAINED_GLASS_PANE, "§a+1"));
        menu.setItem(15, createItem(Material.LIME_STAINED_GLASS_PANE, "§a+8"));

        menu.setItem(18, createItem(Material.ARROW, "§7Back"));
        menu.setItem(22, createItem(Material.EMERALD_BLOCK, "§aConfirm"));

        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        String title = e.getView().getTitle();
        String name = e.getCurrentItem().getItemMeta().getDisplayName();
        e.setCancelled(true);

        if (title.equals("§8RShop")) {
            if (name.contains("Redstone")) openQuantityMenu(player, "redstone");
            else if (name.contains("Piston")) openQuantityMenu(player, "piston");
            else if (name.contains("Observer")) openQuantityMenu(player, "observer");
            else if (name.contains("Hopper")) openQuantityMenu(player, "hopper");

        } else if (title.equals("§8Select Quantity")) {
            Inventory inv = e.getInventory();
            ItemStack centerItem = inv.getItem(13);
            if (centerItem == null || centerItem.getType() == Material.AIR) return;

            String itemName = stripColor(centerItem.getItemMeta().getDisplayName()).toLowerCase();
            int amount = centerItem.getAmount();

            switch (name) {
                case "§a+1" -> amount = Math.min(amount + 1, 64);
                case "§a+8" -> amount = Math.min(amount + 8, 64);
                case "§c-1" -> amount = Math.max(1, amount - 1);
                case "§c-8" -> amount = Math.max(1, amount - 8);
                case "§7Back" -> {
                    openMainMenu(player);
                    return;
                }
                case "§aConfirm" -> {
                    int totalPrice = getItemPrice(itemName) * amount;
                    if (economy.has(player, totalPrice)) {
                        economy.withdrawPlayer(player, totalPrice);
                        player.getInventory().addItem(new ItemStack(getMaterial(itemName), amount));
                        player.sendMessage("§aYou bought: " + amount + "x " + capitalize(itemName) + " §e($" + totalPrice + ")");
                    } else {
                        player.sendMessage("§cYou do not have enough money to buy this! (§e$" + totalPrice + "§c)");
                    }
                    player.closeInventory();
                    return;
                }
            }

            // Update center item
            ItemStack newItem = new ItemStack(getMaterial(itemName), amount);
            ItemMeta meta = newItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + capitalize(itemName));
                newItem.setItemMeta(meta);
            }
            inv.setItem(13, newItem);
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getItemPrice(String name) {
        return switch (name.toLowerCase()) {
            case "redstone" -> 10;
            case "piston" -> 50;
            case "observer" -> 30;
            case "hopper" -> 20;
            default -> 0;
        };
    }

    private Material getMaterial(String name) {
        return switch (name.toLowerCase()) {
            case "redstone" -> Material.REDSTONE;
            case "piston" -> Material.PISTON;
            case "observer" -> Material.OBSERVER;
            case "hopper" -> Material.HOPPER;
            default -> Material.STONE;
        };
    }

    private String stripColor(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}