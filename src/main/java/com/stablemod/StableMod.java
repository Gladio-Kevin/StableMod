package com.stablemod;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class StableMod extends JavaPlugin {
    private Mobs mobs;
    private Bosses bosses;
    private Dungeons dungeons;

    public Mobs mobs() {
        return mobs;
    }

    public Bosses bosses() {
        return bosses;
    }

    @Override
    public void onEnable() {
        Items.init(this);
        for (Minerals.M m : Minerals.ALL) {
            shaped(m, "sword", "M", "M", "S");
            shaped(m, "pickaxe", "MMM", " S ", " S ");
            shaped(m, "axe", "MM", "MS", " S");
            shaped(m, "helmet", "MMM", "M M");
            shaped(m, "chestplate", "M M", "MMM", "MMM");
            shaped(m, "leggings", "MMM", "M M", "M M");
            shaped(m, "boots", "M M", "M M");
        }
        mobs = new Mobs(this);
        bosses = new Bosses(this);
        dungeons = new Dungeons(this);
        getServer().getPluginManager().registerEvents(new Ores(this), this);
        getServer().getPluginManager().registerEvents(new Abilities(this), this);
        getServer().getPluginManager().registerEvents(mobs, this);
        getServer().getPluginManager().registerEvents(bosses, this);
        getServer().getPluginManager().registerEvents(dungeons, this);
        getLogger().info("StableMod aktif!");
    }

    private void shaped(Minerals.M m, String part, String... shape) {
        String id = m.id + "_" + part;
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, id), Items.make(id));
        r.shape(shape);
        r.setIngredient('M', new RecipeChoice.ExactChoice(Items.make(m.id + "_ingot")));
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
            ItemStack it = Items.make(a[1].toLowerCase());
            if (it == null) {
                s.sendMessage("Boyle esya yok.");
                return true;
            }
            t.getInventory().addItem(it);
            s.sendMessage("Verildi: " + a[1]);
            return true;
        }
        if (a.length >= 2 && a[0].equalsIgnoreCase("spawn")) {
            if (!(s instanceof Player pl)) {
                s.sendMessage("Sadece oyuncular kullanabilir.");
                return true;
            }
            LivingEntity e = mobs.spawn(a[1].toLowerCase(), pl.getLocation());
            if (e == null) {
                s.sendMessage("Boyle canavar yok.");
                return true;
            }
            s.sendMessage("Canavar olustu: " + a[1]);
            return true;
        }
        if (a.length >= 1 && a[0].equalsIgnoreCase("dungeon")) {
            if (!(s instanceof Player pl)) {
                s.sendMessage("Sadece oyuncular kullanabilir.");
                return true;
            }
            int floors = 5;
            if (a.length >= 2) {
                try {
                    floors = Math.max(1, Math.min(8, Integer.parseInt(a[1])));
                } catch (NumberFormatException ex) {
                    s.sendMessage("Kat sayisi bir sayi olmali.");
                    return true;
                }
            }
            Location base = pl.getLocation().clone();
            base.setY(base.getBlockY() - 1);
            int built = dungeons.build(base, floors);
            if (built == 0) {
                s.sendMessage("Buraya zindan kurulamadi, daha yuksek bir yer dene.");
            } else {
                s.sendMessage("Zindan kuruldu: " + built + " kat. Kulubenin icindeki merdivenden in.");
            }
            return true;
        }
        s.sendMessage("Kullanim: /stablemod give <esya> [oyuncu]  |  /stablemod spawn <canavar>  |  /stablemod dungeon [kat]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("give", "spawn", "dungeon");
        if (a.length == 2) {
            if (a[0].equalsIgnoreCase("dungeon")) return List.of("3", "5", "8");
            List<String> src = a[0].equalsIgnoreCase("spawn") ? Mobs.IDS : Items.allIds();
            return src.stream().filter(x -> x.startsWith(a[1].toLowerCase())).toList();
        }
        return null;
    }
}
