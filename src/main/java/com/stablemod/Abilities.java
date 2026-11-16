package com.stablemod;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Abilities implements Listener {
    private final NamespacedKey heartKey;
    private final Map<UUID, Location> lights = new HashMap<>();
    private final BlockData lightData;
    private final Random rnd = new Random();

    public Abilities(StableMod plugin) {
        heartKey = new NamespacedKey(plugin, "thobredstone_hearts");
        Light l = (Light) Bukkit.createBlockData(Material.LIGHT);
        l.setLevel(9);
        lightData = l;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private int count(Player p, String mineralId) {
        int n = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            String id = Items.idOf(a);
            if (id != null && id.startsWith(mineralId + "_")) n++;
        }
        return n;
    }

    private boolean isSolnarium(ItemStack it) {
        String id = Items.idOf(it);
        return id != null && id.startsWith("solnarium_");
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateHearts(p);
            updateLight(p);
        }
    }

    private void updateHearts(Player p) {
        AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp == null) return;
        boolean full = count(p, "thobredstone") == 4;
        AttributeModifier existing = null;
        for (AttributeModifier mod : hp.getModifiers()) {
            if (mod.getKey().equals(heartKey)) existing = mod;
        }
        if (full && existing == null) {
            hp.addTransientModifier(new AttributeModifier(
                    heartKey, 10.0, AttributeModifier.Operation.ADD_NUMBER));
        } else if (!full && existing != null) {
            hp.removeModifier(existing);
        }
    }

    private void updateLight(Player p) {
        UUID u = p.getUniqueId();
        Location old = lights.get(u);
        boolean has = count(p, "solnarium") > 0
                || isSolnarium(p.getInventory().getItemInMainHand())
                || isSolnarium(p.getInventory().getItemInOffHand());
        Location now = null;
        if (has) {
            Block b = p.getEyeLocation().getBlock();
            if (b.getType().isAir()) now = b.getLocation();
        }
        if (old != null && !old.equals(now)) {
            p.sendBlockChange(old, old.getBlock().getBlockData());
            lights.remove(u);
        }
        if (now != null) {
            p.sendBlockChange(now, lightData);
            lights.put(u, now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lights.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        String id = Items.idOf(p.getInventory().getItemInMainHand());
        if (id == null || !id.startsWith("darkirtel_")) return;
        if (id.endsWith("_sword") || id.endsWith("_axe") || id.endsWith("_pickaxe")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c != EntityDamageEvent.DamageCause.FIRE
                && c != EntityDamageEvent.DamageCause.FIRE_TICK
                && c != EntityDamageEvent.DamageCause.LAVA
                && c != EntityDamageEvent.DamageCause.HOT_FLOOR) return;
        int n = count(p, "dragonite");
        if (n == 0) return;
        if (n >= 4) {
            e.setCancelled(true);
            p.setFireTicks(0);
        } else {
            e.setDamage(e.getDamage() * (1.0 - 0.25 * n));
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        // Gecici: Thobredstone, kurt adam/vampir gelene kadar cadilardan dusuyor
        if (e.getEntityType() == EntityType.WITCH && rnd.nextInt(100) < 30) {
            ItemStack it = Items.make("thobredstone_ingot");
            if (it != null) e.getDrops().add(it);
        }
    }
}
