package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Bosses implements Listener {
    private final StableMod plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey phaseKey;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Random rnd = new Random();

    public Bosses(StableMod plugin) {
        this.plugin = plugin;
        bossKey = new NamespacedKey(plugin, "boss");
        phaseKey = new NamespacedKey(plugin, "boss_phase");
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    // Canavarlari kat/tier'a gore guclendirir
    public static void boost(LivingEntity e, int tier) {
        AttributeInstance mh = e.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null) {
            mh.setBaseValue(mh.getBaseValue() * (1 + 0.12 * tier));
            e.setHealth(mh.getBaseValue());
        }
        AttributeInstance ad = e.getAttribute(Attribute.ATTACK_DAMAGE);
        if (ad != null) {
            ad.setBaseValue(ad.getBaseValue() * (1 + 0.08 * tier));
        }
    }

    // Mevcut bir canavari boss yapar: boss bari, asamalar, odul sandigi
    public void makeBoss(LivingEntity e, String title, int tier, double hpMul) {
        AttributeInstance mh = e.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null) {
            mh.setBaseValue(mh.getBaseValue() * hpMul);
            e.setHealth(mh.getBaseValue());
        }
        e.customName(Component.text(title, NamedTextColor.GOLD));
        e.setCustomNameVisible(false);
        e.setPersistent(true);
        e.setRemoveWhenFarAway(false);
        e.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, tier + ":" + title);
        ensureBar(e);
    }

    // Zindan Lordu: zindanin son boss'u
    public LivingEntity spawnLord(Location loc, int tier) {
        LivingEntity e = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        setAttr(e, Attribute.MAX_HEALTH, 300);
        e.setHealth(300);
        setAttr(e, Attribute.ATTACK_DAMAGE, 14);
        setAttr(e, Attribute.MOVEMENT_SPEED, 0.3);
        setAttr(e, Attribute.SCALE, 1.8);
        setAttr(e, Attribute.ARMOR, 10);
        setAttr(e, Attribute.KNOCKBACK_RESISTANCE, 0.8);
        setAttr(e, Attribute.FOLLOW_RANGE, 40);
        EntityEquipment eq = e.getEquipment();
        if (eq != null) {
            eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            eq.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
        }
        makeBoss(e, "Zindan Lordu", tier, 1.0 + 0.25 * tier);
        return e;
    }

    private void setAttr(LivingEntity e, Attribute a, double v) {
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(v);
    }

    private void ensureBar(LivingEntity e) {
        if (bars.containsKey(e.getUniqueId())) return;
        String raw = e.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (raw == null) return;
        String title = raw.substring(raw.indexOf(':') + 1);
        BossBar bar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bars.put(e.getUniqueId(), bar);
    }

    private int tierOf(LivingEntity e) {
        String raw = e.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (raw == null) return 0;
        try {
            return Integer.parseInt(raw.substring(0, raw.indexOf(':')));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Entity en : p.getNearbyEntities(48, 24, 48)) {
                if (en instanceof LivingEntity le
                        && le.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {
                    ensureBar(le);
                }
            }
        }

        Iterator<Map.Entry<UUID, BossBar>> it = bars.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BossBar> entry = it.next();
            Entity ent = Bukkit.getEntity(entry.getKey());
            BossBar bar = entry.getValue();
            if (!(ent instanceof LivingEntity le) || le.isDead() || !le.isValid()) {
                bar.removeAll();
                it.remove();
                continue;
            }
            AttributeInstance mh = le.getAttribute(Attribute.MAX_HEALTH);
            double max = mh == null ? 20 : mh.getValue();
            bar.setProgress(Math.max(0.0, Math.min(1.0, le.getHealth() / max)));
            for (Player p : Bukkit.getOnlinePlayers()) {
                boolean near = p.getWorld().equals(le.getWorld())
                        && p.getLocation().distance(le.getLocation()) < 40;
                if (near) bar.addPlayer(p);
                else bar.removePlayer(p);
            }
            phases(le, max);
            slam(le);
        }
    }

    // %66 ve %33 canda uslak cagirir
    private void phases(LivingEntity le, double max) {
        double frac = le.getHealth() / max;
        int phase = le.getPersistentDataContainer().getOrDefault(phaseKey, PersistentDataType.INTEGER, 0);
        int target = frac < 0.33 ? 2 : (frac < 0.66 ? 1 : 0);
        if (target <= phase) return;
        le.getPersistentDataContainer().set(phaseKey, PersistentDataType.INTEGER, target);

        int tier = tierOf(le);
        int count = 2 + tier / 2;
        String id = tier >= 4 ? "seytan" : (tier >= 2 ? "kurt_adam" : "goblin");
        for (int i = 0; i < count; i++) {
            Location l = le.getLocation().add(rnd.nextInt(7) - 3, 0, rnd.nextInt(7) - 3);
            LivingEntity m = plugin.mobs().spawn(id, l);
            if (m != null) {
                m.setPersistent(true);
                m.setRemoveWhenFarAway(false);
                boost(m, tier);
            }
        }
        le.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, target - 1));
        for (Entity en : le.getNearbyEntities(30, 15, 30)) {
            if (en instanceof Player p) {
                p.sendMessage(Component.text("Boss uşaklarını çağırdı!", NamedTextColor.RED));
            }
        }
    }

    // Her ~6 saniyede yer sarsintisi
    private void slam(LivingEntity le) {
        if ((le.getTicksLived() / 10) % 12 != 0) return;
        int tier = tierOf(le);
        for (Entity en : le.getNearbyEntities(5, 3, 5)) {
            if (en instanceof Player p && p.getGameMode() == GameMode.SURVIVAL) {
                p.damage(4 + tier);
                Vector v = p.getLocation().toVector().subtract(le.getLocation().toVector());
                if (v.lengthSquared() > 0) {
                    v.normalize().multiply(1.1).setY(0.5);
                    p.setVelocity(v);
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        String raw = le.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (raw == null) return;
        BossBar bar = bars.remove(le.getUniqueId());
        if (bar != null) bar.removeAll();

        int tier = tierOf(le);
        String title = raw.substring(raw.indexOf(':') + 1);
        e.setDroppedExp(300 + tier * 100);

        Block b = le.getLocation().getBlock();
        if (!b.getType().isAir()) b = b.getRelative(BlockFace.UP);
        b.setType(Material.CHEST);
        if (b.getState() instanceof Chest chest) {
            Dungeons.fillLoot(chest.getBlockInventory(), tier + 2, rnd);
        }
        for (Entity en : le.getNearbyEntities(30, 15, 30)) {
            if (en instanceof Player p) {
                p.sendMessage(Component.text(title + " yenildi! Ödül sandığı açıldı.", NamedTextColor.GOLD));
            }
        }
    }
}
