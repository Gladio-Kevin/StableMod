package com.stablemod;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Buyuk ormanlar: 512x512'lik bolgelerin bir kismi cok uzun agaclarla sik bir ormana donusur.
 * Icinde mini bosslar (Ulu Kurt, Cadi, Dev, Hoggoblin) ve goblin kamplari cikar.
 */
public class Forests implements Listener {
    private final StableMod plugin;
    private final Random rnd = new Random();
    private final BlockData oakL;
    private final BlockData spruceL;
    private final BlockData birchL;
    private final BlockData darkL;

    public Forests(StableMod plugin) {
        this.plugin = plugin;
        oakL = leaves(Material.OAK_LEAVES);
        spruceL = leaves(Material.SPRUCE_LEAVES);
        birchL = leaves(Material.BIRCH_LEAVES);
        darkL = leaves(Material.DARK_OAK_LEAVES);
    }

    private BlockData leaves(Material m) {
        Leaves l = (Leaves) Bukkit.createBlockData(m);
        l.setPersistent(true);
        return l;
    }

    // ------------------------------------------------------------- bolgeler

    private boolean isDemonRegion(World w, int chunkX, int chunkZ) {
        int rx = Math.floorDiv(chunkX, 32);
        int rz = Math.floorDiv(chunkZ, 32);
        if (rx == 0 && rz == 0) return false;
        long h = w.getSeed() * 31L + rx * 341873128712L + rz * 132897987541L;
        h ^= (h >>> 29);
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 32);
        return Math.floorMod(h, 8L) == 0;
    }

    private boolean isForestRegion(World w, int chunkX, int chunkZ) {
        int rx = Math.floorDiv(chunkX, 32);
        int rz = Math.floorDiv(chunkZ, 32);
        if (rx == 0 && rz == 0) return false;
        if (isDemonRegion(w, chunkX, chunkZ)) return false;
        long h = w.getSeed() * 17L + rx * 668265263L + rz * 374761393L;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        return Math.floorMod(h, 5L) == 0;
    }

    private boolean badBiome(String biome) {
        return biome.contains("ocean") || biome.contains("river") || biome.contains("beach")
                || biome.contains("desert") || biome.contains("badlands") || biome.contains("swamp")
                || biome.contains("mushroom");
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        World w = e.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return;
        int chunkX = e.getChunk().getX();
        int chunkZ = e.getChunk().getZ();
        if (!isForestRegion(w, chunkX, chunkZ)) return;
        // yapilar (koy, kale) once kurulsun diye biraz bekle
        Bukkit.getScheduler().runTaskLater(plugin, () -> grow(w, chunkX, chunkZ, false), 10L);
    }

    // ------------------------------------------------------------- ormanlasma

    public void grow(World w, int chunkX, int chunkZ, boolean force) {
        int bx = chunkX * 16;
        int bz = chunkZ * 16;
        int cy = w.getHighestBlockYAt(bx + 8, bz + 8, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (w.getBlockAt(bx + 8, cy, bz + 8).isLiquid()) return;
        String biome = w.getBiome(bx + 8, cy, bz + 8).getKey().getKey();
        if (!force && badBiome(biome)) return;

        boolean camp = rnd.nextInt(45) == 0;

        List<int[]> trunks = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            int x = bx + 4 + rnd.nextInt(8);
            int z = bz + 4 + rnd.nextInt(8);
            boolean close = false;
            for (int[] t : trunks) {
                if (Math.abs(t[0] - x) < 3 && Math.abs(t[1] - z) < 3) close = true;
            }
            if (close) continue;
            if (plantTree(w, x, z, biome)) trunks.add(new int[]{x, z});
        }

        if (camp) {
            buildCamp(w, bx, bz);
            return;
        }
        if (rnd.nextInt(16) == 0) {
            Location l = pick(w, bx, bz, trunks);
            if (l == null) return;
            int r = rnd.nextInt(100);
            if (r < 28) forestMob("ulu_kurt", l);
            else if (r < 48) forestMob("olu_fisildayan", l);
            else if (r < 76) forestMob("orman_cadisi", l);
            else if (r < 90) forestMob("dev", l);
            else forestMob("hoggoblin", l);
        }
    }

    private void forestMob(String id, Location l) {
        LivingEntity m = plugin.mobs().spawn(id, l);
        if (m != null) {
            m.setPersistent(true);
            m.setRemoveWhenFarAway(false);
        }
    }

    private Location pick(World w, int bx, int bz, List<int[]> trunks) {
        for (int t = 0; t < 20; t++) {
            int x = bx + 3 + rnd.nextInt(10);
            int z = bz + 3 + rnd.nextInt(10);
            boolean ok = true;
            for (int[] tr : trunks) {
                if (Math.abs(tr[0] - x) < 3 && Math.abs(tr[1] - z) < 3) ok = false;
            }
            if (!ok) continue;
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (w.getBlockAt(x, y, z).isLiquid()) continue;
            if (!w.getBlockAt(x, y + 1, z).getType().isAir()) continue;
            return new Location(w, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    // ----------------------------------------------------------------- agaclar

    private boolean replaceable(Material t) {
        return t.isAir() || t == Material.SHORT_GRASS || t == Material.TALL_GRASS || t == Material.FERN
                || t == Material.LARGE_FERN || t == Material.SNOW
                || Tag.LEAVES.isTagged(t) || Tag.FLOWERS.isTagged(t);
    }

    private void leaf(World w, int x, int y, int z, BlockData data) {
        Block b = w.getBlockAt(x, y, z);
        Material t = b.getType();
        if (t.isAir() || t == Material.SNOW) b.setBlockData(data, false);
    }

    private void log(World w, int x, int y, int z, Material m) {
        Block b = w.getBlockAt(x, y, z);
        if (replaceable(b.getType())) b.setType(m, false);
    }

    private boolean plantTree(World w, int x, int z, String biome) {
        int type;
        int r = rnd.nextInt(100);
        if (biome.contains("dark")) {
            type = r < 60 ? 3 : (r < 80 ? 1 : 0);
        } else if (biome.contains("taiga") || biome.contains("snowy") || biome.contains("grove")
                || biome.contains("peaks") || biome.contains("frozen")) {
            type = r < 80 ? 0 : 2;
        } else if (biome.contains("birch")) {
            type = r < 70 ? 2 : 1;
        } else {
            type = r < 40 ? 0 : (r < 70 ? 1 : (r < 85 ? 2 : 3));
        }
        if (type == 3) {
            x = Math.min(Math.max(x, (x & ~15) + 5), (x & ~15) + 10);
            z = Math.min(Math.max(z, (z & ~15) + 5), (z & ~15) + 10);
        }

        int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Block ground = w.getBlockAt(x, y, z);
        if (ground.getType() == Material.SNOW) {
            y--;
            ground = w.getBlockAt(x, y, z);
        }
        Material g = ground.getType();
        if (g != Material.GRASS_BLOCK && g != Material.DIRT && g != Material.PODZOL
                && g != Material.COARSE_DIRT && g != Material.ROOTED_DIRT) return false;

        int h = (type == 0 || type == 3) ? 16 + rnd.nextInt(9) : 14 + rnd.nextInt(8);
        if (y + h + 6 >= w.getMaxHeight()) return false;

        Material logM;
        BlockData leafD;
        switch (type) {
            case 0:
                logM = Material.SPRUCE_LOG;
                leafD = spruceL;
                break;
            case 2:
                logM = Material.BIRCH_LOG;
                leafD = birchL;
                break;
            case 3:
                logM = Material.DARK_OAK_LOG;
                leafD = darkL;
                break;
            default:
                logM = Material.OAK_LOG;
                leafD = oakL;
                break;
        }

        // govde
        for (int i = 1; i <= h; i++) {
            log(w, x, y + i, z, logM);
            if (type == 3) {
                log(w, x + 1, y + i, z, logM);
                log(w, x, y + i, z + 1, logM);
                log(w, x + 1, y + i, z + 1, logM);
            }
        }
        if (type == 3 || type == 0) {
            ground.setType(Material.PODZOL, false);
        }

        // yapraklar
        if (type == 0) {
            int low = y + 4;
            int top = y + h + 2;
            for (int ly = low; ly <= top; ly++) {
                double t = (double) (ly - low) / (double) (top - low);
                int rad = (int) Math.round(4.0 * (1.0 - t));
                if ((ly & 1) == 0) rad = Math.max(0, rad - 1);
                for (int dx = -rad; dx <= rad; dx++)
                    for (int dz = -rad; dz <= rad; dz++)
                        if (dx * dx + dz * dz <= rad * rad + 1) leaf(w, x + dx, ly, z + dz, leafD);
            }
        } else {
            int big = (type == 3) ? 5 : 4;
            int cy = y + h - 2;
            for (int dy = -big; dy <= big; dy++) {
                for (int dx = -big; dx <= big + (type == 3 ? 1 : 0); dx++) {
                    for (int dz = -big; dz <= big + (type == 3 ? 1 : 0); dz++) {
                        double d = dx * dx + dz * dz + dy * dy * 1.4;
                        if (d <= big * big + 1 && rnd.nextInt(10) != 0) {
                            leaf(w, x + dx, cy + dy, z + dz, leafD);
                        }
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------ goblin kampi

    private int groundY(World w, int x, int z) {
        int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        while (y > w.getMinHeight()) {
            Material t = w.getBlockAt(x, y, z).getType();
            if (Tag.LOGS.isTagged(t) || Tag.LEAVES.isTagged(t) || t.isAir()) y--;
            else break;
        }
        return y;
    }

    private void buildCamp(World w, int bx, int bz) {
        int cx = bx + 8;
        int cz = bz + 8;
        int yc = groundY(w, cx, cz);
        if (w.getBlockAt(cx, yc, cz).isLiquid()) return;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 17) continue;
                int x = cx + dx;
                int z = cz + dz;
                for (int y = yc + 1; y <= yc + 24; y++) w.getBlockAt(x, y, z).setType(Material.AIR, false);
                for (int y = yc - 2; y <= yc; y++) w.getBlockAt(x, y, z).setType(Material.COARSE_DIRT, false);
            }
        }
        w.getBlockAt(cx, yc + 1, cz).setType(Material.CAMPFIRE, false);

        Block cb = w.getBlockAt(cx + 2, yc + 1, cz);
        cb.setType(Material.CHEST, false);
        if (cb.getState() instanceof Chest c) {
            Dungeons.fillLoot(c.getBlockInventory(), 1, rnd);
        }

        int n = 3 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) {
            int x = cx - 3 + rnd.nextInt(7);
            int z = cz - 3 + rnd.nextInt(7);
            if ((x == cx && z == cz) || (x == cx + 2 && z == cz)) continue;
            forestMob("goblin", new Location(w, x + 0.5, yc + 1, z + 0.5));
        }
        if (rnd.nextInt(6) == 0) {
            forestMob("hoggoblin", new Location(w, cx - 2 + 0.5, yc + 1, cz - 2 + 0.5));
        }
    }
}
