package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Mobs implements Listener {
    public static final List<String> IDS = List.of(
            "ulu_kurt", "olu_fisildayan", "orman_cadisi", "dev", "goblin",
            "hoggoblin", "kurt_adam", "ayi", "seytan", "seytan_kral");

    private final StableMod plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey summonKey;
    private final Random rnd = new Random();

    public Mobs(StableMod plugin) {
        this.plugin = plugin;
        mobKey = new NamespacedKey(plugin, "mob");
        summonKey = new NamespacedKey(plugin, "summoned");
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
    }

    // ---------------------------------------------------------------- spawn

    public LivingEntity spawn(String id, Location loc) {
        switch (id) {
            case "ulu_kurt": {
                LivingEntity e = base(loc, EntityType.WOLF, id, "Ulu Kurt", NamedTextColor.GRAY,
                        20, 7, 0.45, 1.8, true);
                if (e instanceof Wolf w) w.setAngry(true);
                return e;
            }
            case "olu_fisildayan": {
                LivingEntity e = base(loc, EntityType.STRAY, id, "Ölü Fısıldayan", NamedTextColor.DARK_PURPLE,
                        30, 4, 0.25, 1.1, true);
                equip(e, leather(Material.LEATHER_HELMET, Color.fromRGB(20, 20, 30)),
                        leather(Material.LEATHER_CHESTPLATE, Color.fromRGB(20, 20, 30)), null, null, null);
                return e;
            }
            case "orman_cadisi":
                return base(loc, EntityType.WITCH, id, "Orman Cadısı", NamedTextColor.DARK_GREEN,
                        15, 0, 0.25, 1.3, true);
            case "dev": {
                LivingEntity e = base(loc, EntityType.ZOMBIE, id, "Dev", NamedTextColor.DARK_RED,
                        150, 14, 0.17, 2.6, true);
                setAttr(e, Attribute.ARMOR, 8);
                setAttr(e, Attribute.KNOCKBACK_RESISTANCE, 0.6);
                noBurn(e);
                equip(e, null, leather(Material.LEATHER_CHESTPLATE, Color.fromRGB(90, 60, 30)),
                        null, null, new ItemStack(Material.IRON_AXE));
                return e;
            }
            case "goblin": {
                LivingEntity e = base(loc, EntityType.ZOMBIE, id, "Goblin", NamedTextColor.GREEN,
                        12, 3, 0.33, 0.6, false);
                noBurn(e);
                Color g = Color.fromRGB(40, 140, 40);
                equip(e, leather(Material.LEATHER_HELMET, g), leather(Material.LEATHER_CHESTPLATE, g),
                        leather(Material.LEATHER_LEGGINGS, g), leather(Material.LEATHER_BOOTS, g),
                        new ItemStack(Material.STONE_SWORD));
                return e;
            }
            case "hoggoblin": {
                LivingEntity e = base(loc, EntityType.ZOMBIE, id, "Hoggoblin", NamedTextColor.DARK_GREEN,
                        90, 9, 0.2, 1.55, true);
                setAttr(e, Attribute.ARMOR, 6);
                setAttr(e, Attribute.KNOCKBACK_RESISTANCE, 0.4);
                noBurn(e);
                Color g = Color.fromRGB(20, 90, 20);
                equip(e, leather(Material.LEATHER_HELMET, g), leather(Material.LEATHER_CHESTPLATE, g),
                        leather(Material.LEATHER_LEGGINGS, g), leather(Material.LEATHER_BOOTS, g),
                        new ItemStack(Material.IRON_AXE));
                return e;
            }
            case "kurt_adam": {
                LivingEntity e = base(loc, EntityType.ZOMBIE, id, "Kurt Adam", NamedTextColor.GRAY,
                        45, 8, 0.26, 1.25, false);
                noBurn(e);
                Color f = Color.fromRGB(70, 50, 35);
                equip(e, null, leather(Material.LEATHER_CHESTPLATE, f),
                        leather(Material.LEATHER_LEGGINGS, f), null, null);
                return e;
            }
            case "ayi":
                return base(loc, EntityType.POLAR_BEAR, id, "Ayı", NamedTextColor.GRAY,
                        40, 9, 0.3, 1.3, false);
            case "seytan": {
                LivingEntity e = base(loc, EntityType.PIGLIN_BRUTE, id, "Şeytan", NamedTextColor.RED,
                        40, 9, 0.3, 1.2, false);
                if (e instanceof PiglinAbstract pa) pa.setImmuneToZombification(true);
                Color r = Color.fromRGB(140, 10, 10);
                equip(e, leather(Material.LEATHER_HELMET, r), leather(Material.LEATHER_CHESTPLATE, r),
                        null, null, new ItemStack(Material.GOLDEN_AXE));
                return e;
            }
            case "seytan_kral": {
                LivingEntity e = base(loc, EntityType.WITHER_SKELETON, id, "Şeytan Kral", NamedTextColor.DARK_RED,
                        400, 16, 0.28, 2.0, true);
                setAttr(e, Attribute.ARMOR, 10);
                setAttr(e, Attribute.KNOCKBACK_RESISTANCE, 0.9);
                equip(e, new ItemStack(Material.GOLDEN_HELMET),
                        leather(Material.LEATHER_CHESTPLATE, Color.fromRGB(50, 0, 0)),
                        null, null, new ItemStack(Material.NETHERITE_SWORD));
                return e;
            }
            default:
                return null;
        }
    }

    private LivingEntity base(Location loc, EntityType type, String id, String name, NamedTextColor color,
                              double hp, double dmg, double speed, double scale, boolean persistent) {
        LivingEntity e = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        setAttr(e, Attribute.MAX_HEALTH, hp);
        e.setHealth(hp);
        if (dmg > 0) setAttr(e, Attribute.ATTACK_DAMAGE, dmg);
        setAttr(e, Attribute.MOVEMENT_SPEED, speed);
        setAttr(e, Attribute.SCALE, scale);
        setAttr(e, Attribute.FOLLOW_RANGE, 32);
        e.customName(Component.text(name, color));
        e.setCustomNameVisible(true);
        e.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, id);
        e.setPersistent(persistent);
        e.setRemoveWhenFarAway(!persistent);
        return e;
    }

    private void setAttr(LivingEntity e, Attribute a, double v) {
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(v);
    }

    private void noBurn(LivingEntity e) {
        if (e instanceof Zombie z) z.setShouldBurnInDay(false);
    }

    private ItemStack leather(Material m, Color c) {
        ItemStack it = new ItemStack(m);
        LeatherArmorMeta meta = (LeatherArmorMeta) it.getItemMeta();
        meta.setColor(c);
        it.setItemMeta(meta);
        return it;
    }

    private void equip(LivingEntity e, ItemStack head, ItemStack chest, ItemStack legs,
                       ItemStack boots, ItemStack hand) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;
        if (head != null) eq.setHelmet(head);
        if (chest != null) eq.setChestplate(chest);
        if (legs != null) eq.setLeggings(legs);
        if (boots != null) eq.setBoots(boots);
        if (hand != null) eq.setItemInMainHand(hand);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    private String idOf(Entity e) {
        return e.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
    }

    // -------------------------------------------------- dogal olusma (orman)

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        World w = e.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return;
        if (rnd.nextInt(12) != 0) return;
        int x = e.getChunk().getX() * 16 + 8;
        int z = e.getChunk().getZ() * 16 + 8;
        Bukkit.getScheduler().runTask(plugin, () -> {
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (w.getBlockAt(x, y, z).isLiquid()) return;
            String biome = w.getBiome(x, y, z).getKey().getKey();
            if (!biome.contains("forest") && !biome.contains("taiga")) return;
            Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
            int roll = rnd.nextInt(100);
            if (roll < 20) {
                spawn("ulu_kurt", loc);
            } else if (roll < 38) {
                spawn("olu_fisildayan", loc);
            } else if (roll < 56) {
                spawn("orman_cadisi", loc);
            } else if (roll < 86) {
                int n = 3 + rnd.nextInt(3);
                for (int i = 0; i < n; i++) {
                    Location l = loc.clone().add(rnd.nextInt(7) - 3, 0, rnd.nextInt(7) - 3);
                    l.setY(w.getHighestBlockYAt(l, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1);
                    spawn("goblin", l);
                }
            } else if (roll < 97) {
                spawn("dev", loc);
            } else {
                spawn("hoggoblin", loc);
            }
        });
    }

    // ----------------------------------------------------------- yetenekler

    private void tick() {
        Set<UUID> seen = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            for (Entity en : p.getNearbyEntities(32, 16, 32)) {
                if (!(en instanceof LivingEntity m) || !seen.add(m.getUniqueId())) continue;
                String id = idOf(m);
                if (id == null) continue;
                handle(m, id, p);
            }
        }
    }

    private void handle(LivingEntity m, String id, Player p) {
        double d = m.getLocation().distance(p.getLocation());
        switch (id) {
            case "ulu_kurt":
            case "ayi":
                if (m instanceof Mob mob && mob.getTarget() == null && d < 20) mob.setTarget(p);
                break;
            case "kurt_adam": {
                long t = m.getWorld().getTime();
                boolean night = t >= 13000 && t < 23000;
                setAttr(m, Attribute.MOVEMENT_SPEED, night ? 0.38 : 0.26);
                break;
            }
            case "olu_fisildayan":
                if (d < 12) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                    p.sendActionBar(Component.text("Kulağına fısıltılar geliyor...", NamedTextColor.DARK_GRAY));
                }
                break;
            case "orman_cadisi":
                witch(m, p, d);
                break;
            case "seytan_kral":
                king(m, p, d);
                break;
            default:
                break;
        }
    }

    // Cadi: tum iksirleri 2 kat guclu kullanir
    private void witch(LivingEntity m, Player p, double d) {
        if (d > 18 || !m.hasLineOfSight(p)) return;
        if ((m.getTicksLived() / 20) % 3 != 0) return;

        if (rnd.nextInt(4) == 0) {
            List<PotionEffect> buffs = List.of(
                    new PotionEffect(PotionEffectType.SPEED, 400, 1),
                    new PotionEffect(PotionEffectType.REGENERATION, 200, 1),
                    new PotionEffect(PotionEffectType.STRENGTH, 400, 1),
                    new PotionEffect(PotionEffectType.RESISTANCE, 400, 1),
                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
            m.addPotionEffect(buffs.get(rnd.nextInt(buffs.size())));
            return;
        }

        List<PotionEffect> attacks = new ArrayList<>();
        attacks.add(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1));
        attacks.add(new PotionEffect(PotionEffectType.POISON, 800, 1));
        attacks.add(new PotionEffect(PotionEffectType.SLOWNESS, 800, 1));
        attacks.add(new PotionEffect(PotionEffectType.WEAKNESS, 800, 1));
        attacks.add(new PotionEffect(PotionEffectType.WITHER, 400, 1));
        attacks.add(new PotionEffect(PotionEffectType.BLINDNESS, 400, 0));
        attacks.add(new PotionEffect(PotionEffectType.NAUSEA, 400, 0));
        attacks.add(new PotionEffect(PotionEffectType.HUNGER, 600, 1));
        attacks.add(new PotionEffect(PotionEffectType.LEVITATION, 160, 1));
        PotionEffect chosen = attacks.get(rnd.nextInt(attacks.size()));

        ThrownPotion tp = m.launchProjectile(ThrownPotion.class);
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta pm = (PotionMeta) item.getItemMeta();
        pm.addCustomEffect(chosen, true);
        pm.setColor(Color.PURPLE);
        item.setItemMeta(pm);
        tp.setItem(item);
        Vector v = p.getEyeLocation().toVector().subtract(m.getEyeLocation().toVector()).normalize().multiply(0.8);
        v.setY(v.getY() + 0.25);
        tp.setVelocity(v);
    }

    // Seytan Kral: ates aurasi, yariya dusunce 3 seytan cagirir
    private void king(LivingEntity m, Player p, double d) {
        if (d < 6) p.setFireTicks(Math.max(p.getFireTicks(), 60));

        AttributeInstance mh = m.getAttribute(Attribute.MAX_HEALTH);
        double max = mh == null ? 20 : mh.getValue();
        if (m.getHealth() <= max * 0.5
                && !m.getPersistentDataContainer().has(summonKey, PersistentDataType.INTEGER)) {
            m.getPersistentDataContainer().set(summonKey, PersistentDataType.INTEGER, 1);
            for (int i = 0; i < 3; i++) {
                spawn("seytan", m.getLocation().add(rnd.nextInt(5) - 2, 0, rnd.nextInt(5) - 2));
            }
            p.sendMessage(Component.text("Şeytan Kral uşaklarını çağırdı!", NamedTextColor.DARK_RED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity dmg)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        String id = idOf(dmg);
        if (id == null) return;
        if (id.equals("seytan") || id.equals("seytan_kral")) {
            victim.setFireTicks(100);
        }
    }

    // --------------------------------------------------------------- oduller

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        String id = idOf(e.getEntity());
        if (id == null) return;
        switch (id) {
            case "dev":
                drop(e, "oritegold_ingot", 1, 2, 100);
                break;
            case "hoggoblin":
                drop(e, "darkirtel_ingot", 1, 3, 100);
                break;
            case "orman_cadisi":
                drop(e, "magceston_ingot", 1, 2, 70);
                break;
            case "olu_fisildayan":
                drop(e, "solnarium_ingot", 1, 1, 40);
                break;
            case "kurt_adam":
                drop(e, "thobredstone_ingot", 1, 2, 60);
                break;
            case "seytan":
                drop(e, "dragonite_ingot", 1, 1, 20);
                break;
            case "seytan_kral":
                drop(e, "dragonite_ingot", 4, 6, 100);
                drop(e, "mitril_ingot", 2, 3, 100);
                break;
            default:
                break;
        }
    }

    private void drop(EntityDeathEvent e, String itemId, int min, int max, int chance) {
        if (rnd.nextInt(100) >= chance) return;
        ItemStack it = Items.make(itemId);
        if (it == null) return;
        it.setAmount(min + rnd.nextInt(max - min + 1));
        e.getDrops().add(it);
    }
}
