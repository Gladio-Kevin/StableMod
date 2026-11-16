package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class Dungeons implements Listener {
    private final StableMod plugin;
    private final NamespacedKey trapKey;
    private final Random rnd = new Random();

    public Dungeons(StableMod plugin) {
        this.plugin = plugin;
        trapKey = new NamespacedKey(plugin, "traps");
    }

    // ------------------------------------------------------------ yardimcilar

    private void set(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    private void fill(World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    set(w, x, y, z, m);
    }

    private Material wall() {
        int r = rnd.nextInt(10);
        return r < 6 ? Material.STONE_BRICKS : (r < 8 ? Material.CRACKED_STONE_BRICKS : Material.MOSSY_STONE_BRICKS);
    }

    private void ladder(World w, int x, int y, int z) {
        Ladder l = (Ladder) Material.LADDER.createBlockData();
        l.setFacing(BlockFace.WEST);
        w.getBlockAt(x, y, z).setBlockData(l, false);
    }

    // ------------------------------------------------------------------ insa

    /**
     * Zindan kurar. base = yuzeydeki blok (kulubenin zemini).
     * Her kat 17x17, katlar arasi 7 blok. Donen deger: kurulan kat sayisi.
     */
    public int build(Location base, int floors) {
        World w = base.getWorld();
        int hx = base.getBlockX();
        int hz = base.getBlockZ();
        int sy = base.getBlockY();
        int sx = hx + 1;
        int sz = hz;
        int cx = sx - 7;
        int cz = sz - 7;

        int maxFloors = (sy - 12 - (w.getMinHeight() + 2)) / 7 + 1;
        floors = Math.min(floors, maxFloors);
        if (floors < 1) return 0;

        // 1) odalar
        for (int i = 0; i < floors; i++) {
            buildRoom(w, cx, cz, sy - 12 - 7 * i);
        }

        // 2) katlar arasi delikler ve merdivenler
        for (int i = 0; i < floors - 1; i++) {
            int fy = sy - 12 - 7 * i;
            int lower = fy - 7;
            for (int y = fy - 1; y <= fy; y++) set(w, sx, y, sz, Material.AIR);
            for (int y = lower + 1; y <= fy + 1; y++) ladder(w, sx, y, sz);
        }

        // 3) yuzeyden ilk odaya inen kuyu
        int f0 = sy - 12;
        for (int y = f0 + 6; y <= sy; y++) {
            set(w, sx, y, sz, Material.AIR);
            set(w, sx + 1, y, sz, wall());
            set(w, sx - 1, y, sz, wall());
            set(w, sx, y, sz - 1, wall());
            set(w, sx, y, sz + 1, wall());
        }

        // 4) giris kulubesi
        buildHut(w, hx, hz, sy);
        for (int y = f0 + 1; y <= sy + 1; y++) ladder(w, sx, y, sz);

        // 5) icini doldur: canavarlar, sandiklar, tuzaklar, bosslar
        for (int i = 0; i < floors; i++) {
            int fy = sy - 12 - 7 * i;
            boolean finalFloor = (i == floors - 1);
            boolean midBoss = (i == 2 && floors >= 4 && !finalFloor);
            Location center = new Location(w, cx + 0.5, fy + 1, cz + 0.5);

            int mobCount = Math.min(3 + i * 2, 14);
            if (finalFloor) mobCount = mobCount / 2 + 1;
            for (int k = 0; k < mobCount; k++) {
                spawnMob(pickMob(i), randomSpot(w, cx, cz, fy + 1, sx, sz), i);
            }

            for (int k = 0; k < 2; k++) placeChest(w, cx, cz, fy, sx, sz, i);

            int traps = 3 + i;
            for (int k = 0; k < traps; k++) {
                Location t = randomSpot(w, cx, cz, fy + 1, sx, sz);
                Block b = t.getBlock();
                if (b.getType().isAir()) {
                    b.setType(Material.STONE_PRESSURE_PLATE, false);
                    registerTrap(b, i);
                }
            }

            if (midBoss) {
                LivingEntity h = plugin.mobs().spawn("hoggoblin", center);
                if (h != null) plugin.bosses().makeBoss(h, "Hoggoblin Şefi", i, 1.0 + 0.25 * i);
            }
            if (finalFloor) {
                plugin.bosses().spawnLord(center, i);
            }
        }
        return floors;
    }

    private void buildRoom(World w, int cx, int cz, int fy) {
        for (int x = cx - 8; x <= cx + 8; x++)
            for (int z = cz - 8; z <= cz + 8; z++)
                for (int y = fy; y <= fy + 6; y++)
                    set(w, x, y, z, wall());
        fill(w, cx - 7, fy + 1, cz - 7, cx + 7, fy + 5, cz + 7, Material.AIR);

        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-3, 3}) {
                fill(w, cx + dx, fy + 1, cz + dz, cx + dx, fy + 5, cz + dz, Material.CHISELED_STONE_BRICKS);
            }
        }
        set(w, cx, fy + 6, cz, Material.GLOWSTONE);
        for (int dx : new int[]{-5, 5}) {
            for (int dz : new int[]{-5, 5}) {
                set(w, cx + dx, fy + 6, cz + dz, Material.GLOWSTONE);
            }
        }
    }

    private void buildHut(World w, int hx, int hz, int sy) {
        fill(w, hx - 2, sy, hz - 2, hx + 2, sy, hz + 2, Material.STONE_BRICKS);
        fill(w, hx - 2, sy + 1, hz - 2, hx + 2, sy + 4, hz + 2, Material.AIR);
        for (int y = sy + 1; y <= sy + 3; y++) {
            for (int x = hx - 2; x <= hx + 2; x++) {
                set(w, x, y, hz - 2, wall());
                set(w, x, y, hz + 2, wall());
            }
            for (int z = hz - 2; z <= hz + 2; z++) {
                set(w, hx - 2, y, z, wall());
                set(w, hx + 2, y, z, wall());
            }
        }
        fill(w, hx - 2, sy + 4, hz - 2, hx + 2, sy + 4, hz + 2, Material.STONE_BRICKS);
        // kapi (bati tarafi)
        set(w, hx - 2, sy + 1, hz, Material.AIR);
        set(w, hx - 2, sy + 2, hz, Material.AIR);
        // kuyu deligi
        set(w, hx + 1, sy, hz, Material.AIR);
        set(w, hx, sy + 1, hz + 1, Material.TORCH);
    }

    private Location randomSpot(World w, int cx, int cz, int y, int sx, int sz) {
        int x;
        int z;
        do {
            x = cx - 7 + rnd.nextInt(15);
            z = cz - 7 + rnd.nextInt(15);
        } while (x == sx && z == sz);
        return new Location(w, x + 0.5, y, z + 0.5);
    }

    private void placeChest(World w, int cx, int cz, int fy, int sx, int sz, int depth) {
        int x;
        int z;
        if (rnd.nextBoolean()) {
            x = rnd.nextBoolean() ? cx - 7 : cx + 7;
            z = cz - 7 + rnd.nextInt(15);
        } else {
            z = rnd.nextBoolean() ? cz - 7 : cz + 7;
            x = cx - 7 + rnd.nextInt(15);
        }
        if (x == sx && z == sz) return;
        Block b = w.getBlockAt(x, fy + 1, z);
        if (!b.getType().isAir()) return;
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest chest) {
            fillLoot(chest.getBlockInventory(), depth, rnd);
        }
    }

    // ----------------------------------------------------------- canavarlar

    private String pickMob(int floor) {
        int r = rnd.nextInt(100);
        if (floor <= 1) {
            if (r < 60) return "goblin";
            return r < 85 ? "vanilla:ZOMBIE" : "vanilla:SKELETON";
        }
        if (floor <= 3) {
            if (r < 35) return "kurt_adam";
            if (r < 65) return "olu_fisildayan";
            if (r < 85) return "goblin";
            return "ayi";
        }
        if (r < 25) return "hoggoblin";
        if (r < 50) return "kurt_adam";
        if (r < 75) return "olu_fisildayan";
        return "seytan";
    }

    private LivingEntity spawnMob(String id, Location loc, int depth) {
        LivingEntity e;
        if (id.startsWith("vanilla:")) {
            e = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.valueOf(id.substring(8)));
        } else {
            e = plugin.mobs().spawn(id, loc);
        }
        if (e == null) return null;
        e.setPersistent(true);
        e.setRemoveWhenFarAway(false);
        Bosses.boost(e, depth);
        return e;
    }

    // ---------------------------------------------------------------- tuzaklar

    private void registerTrap(Block b, int depth) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        String data = pdc.getOrDefault(trapKey, PersistentDataType.STRING, ";");
        data = data + b.getX() + "," + b.getY() + "," + b.getZ() + ":" + depth + ";";
        pdc.set(trapKey, PersistentDataType.STRING, data);
    }

    @EventHandler
    public void onStep(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.STONE_PRESSURE_PLATE) return;

        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        String data = pdc.getOrDefault(trapKey, PersistentDataType.STRING, "");
        String search = ";" + b.getX() + "," + b.getY() + "," + b.getZ() + ":";
        int index = data.indexOf(search);
        if (index < 0) return;
        int end = data.indexOf(';', index + 1);
        if (end < 0) return;
        int depth;
        try {
            depth = Integer.parseInt(data.substring(index + search.length(), end));
        } catch (NumberFormatException ex) {
            return;
        }
        pdc.set(trapKey, PersistentDataType.STRING, data.substring(0, index + 1) + data.substring(end + 1));
        trigger(e.getPlayer(), depth);
    }

    private void trigger(Player p, int depth) {
        p.sendActionBar(Component.text("Bir tuzağa bastın!", NamedTextColor.RED));
        Location pl = p.getLocation();
        switch (rnd.nextInt(4)) {
            case 0:
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4;
                    Location from = pl.clone().add(Math.cos(ang) * 4, 1.5, Math.sin(ang) * 4);
                    Vector dir = p.getEyeLocation().toVector().subtract(from.toVector());
                    if (dir.lengthSquared() == 0) continue;
                    Arrow a = p.getWorld().spawnArrow(from, dir, 1.6f, 4f);
                    a.setDamage(2 + depth);
                    a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                }
                break;
            case 1:
                p.setFireTicks(100 + depth * 20);
                break;
            case 2:
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120 + depth * 20, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 0));
                break;
            default:
                int n = 2 + depth / 2;
                for (int i = 0; i < n; i++) {
                    Location l = pl.clone().add(rnd.nextInt(7) - 3, 0, rnd.nextInt(7) - 3);
                    spawnMob(pickMob(depth), l, depth);
                }
                p.sendMessage(Component.text("Pusu! Canavarlar etrafını sardı!", NamedTextColor.RED));
                break;
        }
    }

    // ------------------------------------------------------ dogal olusma

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        World w = e.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return;
        if (rnd.nextInt(350) != 0) return;
        int x = e.getChunk().getX() * 16 + 8;
        int z = e.getChunk().getZ() * 16 + 8;
        Bukkit.getScheduler().runTask(plugin, () -> {
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (w.getBlockAt(x, y, z).isLiquid()) return;
            String biome = w.getBiome(x, y, z).getKey().getKey();
            if (biome.contains("ocean") || biome.contains("river") || biome.contains("beach")) return;
            build(new Location(w, x, y, z), 4 + rnd.nextInt(3));
        });
    }

    // --------------------------------------------------------------- oduller

    /** Derinlige gore sandik doldurur. Boss sandiklari yuksek depth ile cagirir. */
    public static void fillLoot(Inventory inv, int depth, Random rnd) {
        int tier = Math.min(depth / 2, 3);
        int stacks = 4 + tier * 2;
        for (int i = 0; i < stacks; i++) {
            ItemStack it = rollLoot(tier, rnd);
            inv.setItem(rnd.nextInt(inv.getSize()), it);
        }
    }

    private static ItemStack custom(String id, int min, int max, Random rnd) {
        ItemStack it = Items.make(id);
        it.setAmount(min + rnd.nextInt(max - min + 1));
        return it;
    }

    private static ItemStack rollLoot(int tier, Random rnd) {
        int r = rnd.nextInt(100);
        switch (tier) {
            case 0:
                if (r < 30) return new ItemStack(Material.BREAD, 2 + rnd.nextInt(4));
                if (r < 55) return new ItemStack(Material.IRON_INGOT, 1 + rnd.nextInt(4));
                if (r < 70) return new ItemStack(Material.ARROW, 8 + rnd.nextInt(16));
                if (r < 85) return new ItemStack(Material.GOLD_INGOT, 1 + rnd.nextInt(3));
                return custom("oritegold_ingot", 1, 2, rnd);
            case 1:
                if (r < 25) return custom("darkirtel_ingot", 1, 2, rnd);
                if (r < 45) return custom("magceston_ingot", 1, 3, rnd);
                if (r < 60) return custom("oritegold_ingot", 1, 3, rnd);
                if (r < 75) return new ItemStack(Material.DIAMOND, 1 + rnd.nextInt(2));
                if (r < 90) return new ItemStack(Material.GOLDEN_APPLE, 1);
                return new ItemStack(Material.IRON_INGOT, 3 + rnd.nextInt(4));
            case 2:
                if (r < 20) return custom("solnarium_ingot", 1, 2, rnd);
                if (r < 40) return custom("darkirtel_ingot", 2, 3, rnd);
                if (r < 55) return custom("magceston_ingot", 2, 3, rnd);
                if (r < 75) return new ItemStack(Material.DIAMOND, 2 + rnd.nextInt(3));
                if (r < 90) return new ItemStack(Material.GOLDEN_APPLE, 1 + rnd.nextInt(2));
                return custom("thobredstone_ingot", 1, 1, rnd);
            default:
                if (r < 20) return custom("dragonite_ingot", 1, 2, rnd);
                if (r < 40) return custom("mitril_ingot", 1, 2, rnd);
                if (r < 55) return custom("solnarium_ingot", 2, 3, rnd);
                if (r < 75) return new ItemStack(Material.DIAMOND, 3 + rnd.nextInt(4));
                if (r < 90) return new ItemStack(Material.GOLDEN_APPLE, 2 + rnd.nextInt(2));
                if (r < 97) return custom("thobredstone_ingot", 1, 2, rnd);
                return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
        }
    }
}
