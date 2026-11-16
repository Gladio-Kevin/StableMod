package com.stablemod;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class StableMod extends JavaPlugin {
    @Override
    public void onEnable() {
        Items.init(this);
        shaped("mitril_sword", "M", "M", "S");
        shaped("mitril_pickaxe", "MMM", " S ", " S ");
        shaped("mitril_axe", "MM", "MS", " S");
        shaped("mitril_helmet", "MMM", "M M");
        shaped("mitril_chestplate", "M M", "MMM", "MMM");
        shaped("mitril_leggings", "MMM", "M M", "M M");
        shaped("mitril_boots", "M M", "M M");
        getServer().getPluginManager().registerEvents(new MitrilOre(this), this);
        getLogger().info("StableMod aktif!");
    }

    private void shaped(String id, String... shape) {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, id), Items.make(id));
        r.shape(shape);
        r.setIngredient('M', new RecipeChoice.ExactChoice(Items.make("mitril_ingot")));
        if (String.join("", shape).contains("S")) {
            r.setIngredient('S', Material.STICK);
        }
        Bukkit.addRecipe(r);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length >= 2 && a[0].equalsIgnoreCase("give")) {
            Player t = a.length >= 3 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player p ? p : null);
            if (t == null) {
                s.sendMessage("Oyuncu bulunamadi.");
                return true;
            }
            ItemStack it = Items.make(a[1]);
            if (it == null) {
                s.sendMessage("Boyle esya yok.");
                return true;
            }
            t.getInventory().addItem(it);
            s.sendMessage("Verildi: " + a[1]);
            return true;
        }
        s.sendMessage("Kullanim: /stablemod give <esya> [oyuncu]");
        return true;
    }
}
