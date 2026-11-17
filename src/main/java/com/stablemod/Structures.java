package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Structures implements Listener {
    public static final List<String> TYPES = List.of(
            "kule", "kale", "seytan_kalesi", "insan_koyu", "cuce_koyu", "elf_koyu", "magara");

    private final StableMod plugin;
    private final Random rnd = new Random();

    public Structures(StableMod plugin) {
        this.plugin = plugin;
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

    private Material wall(boolean demon) {
        int r = rnd.nextInt(10);
        if (demon) {
            if (r < 6) return Material.POLISHED_BLACKSTONE_BRICKS;
            if (r < 8) return Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
            return r == 8 ? Material.NETHER_BRICKS : Material.RED_NETHER_BRICKS;
        }
        return r < 6 ? Material.STONE_BRICKS : (r < 8 ? Material.CRACKED_STONE_BRICKS : Material.MOSSY_STONE_BRICKS);
    }

    private void ladder(World w, int x, int y, int z) {
        Ladder l = (Ladder) Material.LADDER.createBlockData();
        l.setFacing(BlockFace.WEST);
        w.getBlockAt(x, y, z).setBlockData(l, false);
    }

    private void setAttr(LivingEntity e, Attribute a, double v) {
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(v);
    }

    private boolean free(World w, int x, int y, int z) {
        return w.getBlockAt(x, y, z).getType().isAir() && w.getBlockAt(x, y + 1, z).getType().isAir();
    }

    private Location spot(World w, int cx, int cz, int half, int y) {
        for (int t = 0; t < 30; t++) {
            int x = cx - half + rnd.nextInt(half * 2 + 1);
            int z = cz - half + rnd.nextInt(half * 2 + 1);
            if (free(w, x, y, z)) return new Location(w, x + 0.5, y, z + 0.5);
        }
        return null;
    }

    private void chest(World w, int x, int y, int z, int depth) {
        Block b = w.getBlockAt(x, y, z);
        if (!b.getType().isAir()) return;
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest c) {
            Dungeons.fillLoot(c.getBlockInventory(), depth, rnd);
        }
    }

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

    private LivingEntity spawnHostile(String id, Location loc, int depth) {
        if (loc == null) return null;
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

    // ------------------------------------------------------ askerler ve koyluler

    public LivingEntity spawnNpc(String id, Location loc) {
        switch (id) {
            case "asker":
                return soldier(loc);
            case "insan_koylu":
                return folk(loc, "human");
            case "cuce":
                return folk(loc, "dwarf");
            case "elf_koylu":
                return folk(loc, "elf");
            default:
                return null;
        }
    }

    private LivingEntity soldier(Location loc) {
        if (loc == null) return null;
        IronGolem g = (IronGolem) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
        g.setPlayerCreated(true);
        setAttr(g, Attribute.MAX_HEALTH, 80);
        g.setHealth(80);
        setAttr(g, Attribute.ATTACK_DAMAGE, 12);
        setAttr(g, Attribute.SCALE, 0.9);
        g.customName(Component.text("Kale Askeri", NamedTextColor.AQUA));
        g.setCustomNameVisible(true);
        g.setPersistent(true);
        g.setRemoveWhenFarAway(false);
        return g;
    }

    private LivingEntity folk(Location loc, String race) {
        if (loc == null) return null;
        Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        String name;
        NamedTextColor color;
        double scale;
        Villager.Profession prof;
        int r = rnd.nextInt(3);
        switch (race) {
            case "dwarf":
                name = "Cüce";
                color = NamedTextColor.GOLD;
                scale = 0.75;
                prof = r == 0 ? Villager.Profession.ARMORER : (r == 1 ? Villager.Profession.WEAPONSMITH : Villager.Profession.TOOLSMITH);
                break;
            case "elf":
                name = "Elf";
                color = NamedTextColor.GREEN;
                scale = 1.1;
                prof = r == 0 ? Villager.Profession.LIBRARIAN : (r == 1 ? Villager.Profession.CLERIC : Villager.Profession.FLETCHER);
                break;
            default:
                name = "İnsan Köylü";
                color = NamedTextColor.WHITE;
                scale = 1.0;
                prof = r == 0 ? Villager.Profession.FARMER : (r == 1 ? Villager.Profession.BUTCHER : Villager.Profession.FLETCHER);
                break;
        }
        v.customName(Component.text(name, color));
        v.setCustomNameVisible(true);
        v.setProfession(prof);
        v.setVillagerLevel(2);
        v.setVillagerExperience(10);
        v.setRecipes(trades(race));
        setAttr(v, Attribute.SCALE, scale);
        v.setPersistent(true);
        v.setRemoveWhenFarAway(false);
        return v;
    }

    private MerchantRecipe trade(ItemStack result, int uses, ItemStack cost) {
        MerchantRecipe r = new MerchantRecipe(result, uses);
        r.addIngredient(cost);
        return r;
    }

    private List<MerchantRecipe> trades(String race) {
        List<MerchantRecipe> list = new ArrayList<>();
        switch (race) {
            case "dwarf":
                list.add(trade(Items.make("oritegold_ingot"), 3, new ItemStack(Material.IRON_INGOT, 24)));
                list.add(trade(Items.make("darkirtel_ingot"), 2, new ItemStack(Material.DIAMOND, 6)));
                list.add(trade(new ItemStack(Material.EMERALD, 2), 10, new ItemStack(Material.IRON_INGOT, 8)));
                break;
            case "elf":
                list.add(trade(Items.make("solnarium_ingot"), 2, new ItemStack(Material.EMERALD, 16)));
                list.add(trade(Items.make("magceston_ingot"), 3, new ItemStack(Material.GOLD_INGOT, 16)));
                list.add(trade(new ItemStack(Material.GOLDEN_APPLE, 1), 4, new ItemStack(Material.EMERALD, 8)));
                break;
            default:
                list.add(trade(new ItemStack(Material.BREAD, 8), 16, new ItemStack(Material.EMERALD, 1)));
                list.add(trade(new ItemStack(Material.EMERALD, 1), 16, new ItemStack(Material.IRON_INGOT, 4)));
                list.add(trade(new ItemStack(Material.DIAMOND, 1), 4, new ItemStack(Material.EMERALD, 12)));
                break;
        }
        return list;
    }

    // ---------------------------------------------------------------- build

    public boolean build(String type, Location base) {
        switch (type) {
            case "kule":
                tower(base, 4 + rnd.nextInt(4));
                return true;
            case "kale":
                castle(base, false);
                return true;
            case "seytan_kalesi":
                castle(base, true);
                return true;
            case "insan_koyu":
                village(base, "human");
                return true;
            case "cuce_koyu":
                village(base, "dwarf");
                return true;
            case "elf_koyu":
                village(base, "elf");
                return true;
            case "magara":
                return lair(base);
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------ kule

    private void tower(Location base, int floors) {
        World w = base.getWorld();
        int tx = base.getBlockX();
        int tz = base.getBlockZ();
        int sy = base.getBlockY();
        int height = floors * 6;

        fill(w, tx - 6, sy + 1, tz - 6, tx + 6, sy + height + 3, tz + 6, Material.AIR);
        fill(w, tx - 6, sy - 3, tz - 6, tx + 6, sy - 1, tz + 6, Material.COBBLESTONE);
        for (int x = tx - 5; x <= tx + 5; x++)
            for (int z = tz - 5; z <= tz + 5; z++)
                for (int y = sy; y <= sy + height; y++)
                    set(w, x, y, z, wall(false));

        for (int i = 0; i < floors; i++) {
            int fy = sy + 6 * i;
            fill(w, tx - 4, fy + 1, tz - 4, tx + 4, fy + 5, tz + 4, Material.AIR);
            set(w, tx, fy + 6, tz, Material.GLOWSTONE);
            set(w, tx - 5, fy + 3, tz, Material.AIR);
            set(w, tx, fy + 3, tz - 5, Material.AIR);
        }
        for (int i = 0; i < floors - 1; i++) {
            int fy = sy + 6 * i;
            set(w, tx + 4, fy + 6, tz + 4, Material.AIR);
            for (int y = fy + 1; y <= fy + 7; y++) ladder(w, tx + 4, y, tz + 4);
        }
        // kapi
        set(w, tx, sy + 1, tz + 5, Material.AIR);
        set(w, tx, sy + 2, tz + 5, Material.AIR);
        // mazgallar
        for (int x = tx - 5; x <= tx + 5; x++) {
            for (int z = tz - 5; z <= tz + 5; z++) {
                boolean ring = Math.abs(x - tx) == 5 || Math.abs(z - tz) == 5;
                if (ring && (x + z) % 2 == 0) set(w, x, sy + height + 1, z, wall(false));
            }
        }

        for (int i = 0; i < floors; i++) {
            int fy = sy + 6 * i;
            boolean top = (i == floors - 1);
            int n = top ? 2 : Math.min(2 + i, 9);
            for (int k = 0; k < n; k++) {
                spawnHostile(pickMob(i), spot(w, tx, tz, 3, fy + 1), i);
            }
            chest(w, tx - 4, fy + 1, tz - 3 + rnd.nextInt(7), i);
            if (top) {
                Location c = new Location(w, tx + 0.5, fy + 1, tz + 0.5);
                LivingEntity d = plugin.mobs().spawn("dev", c);
                if (d != null) plugin.bosses().makeBoss(d, "Kule Devi", floors, 1.0 + 0.3 * floors);
            }
        }
    }

    // ------------------------------------------------------------------ kale

    private void castle(Location base, boolean demon) {
        World w = base.getWorld();
        int cx = base.getBlockX();
        int cz = base.getBlockZ();
        int sy = base.getBlockY();
        Material floor = demon ? Material.BLACKSTONE : Material.COBBLESTONE;
        Material lantern = demon ? Material.SOUL_LANTERN : Material.LANTERN;
        Material post = demon ? Material.POLISHED_BLACKSTONE_WALL : Material.COBBLESTONE_WALL;
        Material light = demon ? Material.SHROOMLIGHT : Material.GLOWSTONE;

        fill(w, cx - 17, sy + 1, cz - 17, cx + 17, sy + 16, cz + 17, Material.AIR);
        fill(w, cx - 17, sy - 4, cz - 17, cx + 17, sy - 1, cz + 17, floor);
        fill(w, cx - 15, sy, cz - 15, cx + 15, sy, cz + 15, floor);

        // dis surlar
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                if (Math.abs(x) >= 14 || Math.abs(z) >= 14) {
                    for (int y = sy + 1; y <= sy + 8; y++) set(w, cx + x, y, cz + z, wall(demon));
                    if ((x + z) % 2 == 0) set(w, cx + x, sy + 9, cz + z, wall(demon));
                }
            }
        }
        // kose kuleleri
        for (int sxm : new int[]{-1, 1}) {
            for (int szm : new int[]{-1, 1}) {
                int tcx = cx + 14 * sxm;
                int tcz = cz + 14 * szm;
                for (int x = tcx - 3; x <= tcx + 3; x++)
                    for (int z = tcz - 3; z <= tcz + 3; z++)
                        for (int y = sy + 1; y <= sy + 13; y++)
                            set(w, x, y, z, wall(demon));
                fill(w, tcx - 2, sy + 1, tcz - 2, tcx + 2, sy + 12, tcz + 2, Material.AIR);
                for (int x = tcx - 3; x <= tcx + 3; x++) {
                    for (int z = tcz - 3; z <= tcz + 3; z++) {
                        boolean ring = Math.abs(x - tcx) == 3 || Math.abs(z - tcz) == 3;
                        if (ring && (x + z) % 2 == 0) set(w, x, sy + 14, z, wall(demon));
                    }
                }
                set(w, tcx - 3 * sxm, sy + 1, tcz - szm, Material.AIR);
                set(w, tcx - 3 * sxm, sy + 2, tcz - szm, Material.AIR);
            }
        }
        // kapi (guney)
        fill(w, cx - 1, sy + 1, cz + 14, cx + 1, sy + 4, cz + 15, Material.AIR);

        // ana bina
        for (int x = cx - 5; x <= cx + 5; x++)
            for (int z = cz - 11; z <= cz - 1; z++)
                for (int y = sy + 1; y <= sy + 12; y++)
                    set(w, x, y, z, wall(demon));
        fill(w, cx - 4, sy + 1, cz - 10, cx + 4, sy + 11, cz - 2, Material.AIR);
        set(w, cx, sy + 1, cz - 1, Material.AIR);
        set(w, cx, sy + 2, cz - 1, Material.AIR);
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 11; z <= cz - 1; z++) {
                boolean ring = x == cx - 5 || x == cx + 5 || z == cz - 11 || z == cz - 1;
                if (ring && (x + z) % 2 == 0) set(w, x, sy + 13, z, wall(demon));
            }
        }
        set(w, cx, sy + 12, cz - 6, light);
        set(w, cx - 2, sy + 12, cz - 4, light);
        set(w, cx + 2, sy + 12, cz - 4, light);
        set(w, cx - 2, sy + 12, cz - 8, light);
        set(w, cx + 2, sy + 12, cz - 8, light);

        // avlu isiklari
        for (int dx : new int[]{-8, 8}) {
            for (int dz : new int[]{-8, 8}) {
                set(w, cx + dx, sy + 1, cz + dz, post);
                set(w, cx + dx, sy + 2, cz + dz, post);
                set(w, cx + dx, sy + 3, cz + dz, lantern);
            }
        }

        // sandiklar
        chest(w, cx - 3, sy + 1, cz - 10, demon ? 7 : 3);
        chest(w, cx + 3, sy + 1, cz - 10, demon ? 7 : 3);

        if (!demon) {
            for (int i = 0; i < 6; i++) soldier(spot(w, cx, cz + 7, 6, sy + 1));
            for (int i = 0; i < 3; i++) soldier(spot(w, cx, cz - 6, 3, sy + 1));
            for (int i = 0; i < 5; i++) folk(spot(w, cx, cz + 7, 6, sy + 1), "human");
            for (int i = 0; i < 2; i++) folk(spot(w, cx, cz - 6, 3, sy + 1), "human");
        } else {
            plugin.mobs().spawn("seytan_kral", new Location(w, cx + 0.5, sy + 1, cz - 8 + 0.5));
            for (int i = 0; i < 4; i++) spawnHostile("seytan", spot(w, cx, cz - 6, 3, sy + 1), 3);
            for (int i = 0; i < 4; i++) spawnHostile("seytan", spot(w, cx, cz + 7, 6, sy + 1), 3);
        }
    }

    // ------------------------------------------------------------------ koy

    private void village(Location base, String race) {
        World w = base.getWorld();
        int cx = base.getBlockX();
        int cz = base.getBlockZ();
        int sy = base.getBlockY();
        Material plaza;
        Material wallM;
        Material pillar;
        Material roof;
        Material floorM;
        switch (race) {
            case "dwarf":
                plaza = Material.COBBLED_DEEPSLATE;
                wallM = Material.STONE_BRICKS;
                pillar = Material.POLISHED_DEEPSLATE;
                roof = Material.DEEPSLATE_TILES;
                floorM = Material.STONE;
                break;
            case "elf":
                plaza = Material.COARSE_DIRT;
                wallM = Material.BIRCH_PLANKS;
                pillar = Material.BIRCH_LOG;
                roof = Material.CHERRY_PLANKS;
                floorM = Material.SPRUCE_PLANKS;
                break;
            default:
                plaza = Material.DIRT_PATH;
                wallM = Material.OAK_PLANKS;
                pillar = Material.OAK_LOG;
                roof = Material.SPRUCE_PLANKS;
                floorM = Material.COBBLESTONE;
                break;
        }
        Material[] mats = {wallM, pillar, roof, floorM};

        // meydan
        fill(w, cx - 4, sy - 3, cz - 4, cx + 4, sy - 1, cz + 4, Material.COBBLESTONE);
        fill(w, cx - 4, sy + 1, cz - 4, cx + 4, sy + 6, cz + 4, Material.AIR);
        fill(w, cx - 4, sy, cz - 4, cx + 4, sy, cz + 4, plaza);
        fill(w, cx - 1, sy, cz - 1, cx + 1, sy, cz + 1, Material.STONE_BRICKS);
        fill(w, cx - 1, sy + 1, cz - 1, cx + 1, sy + 1, cz + 1, Material.STONE_BRICKS);
        set(w, cx, sy + 1, cz, Material.WATER);
        for (int dx : new int[]{-4, 4}) {
            for (int dz : new int[]{-4, 4}) {
                set(w, cx + dx, sy + 1, cz + dz, Material.COBBLESTONE_WALL);
                set(w, cx + dx, sy + 2, cz + dz, Material.COBBLESTONE_WALL);
                set(w, cx + dx, sy + 3, cz + dz, Material.LANTERN);
            }
        }

        // evler
        List<int[]> spots = new ArrayList<>();
        spots.add(new int[]{-10, 0});
        spots.add(new int[]{10, 0});
        spots.add(new int[]{0, -10});
        spots.add(new int[]{0, 10});
        int[][] diag = {{-10, -10}, {10, 10}, {-10, 10}, {10, -10}};
        for (int[] d : diag) {
            if (rnd.nextBoolean()) spots.add(d);
        }
        for (int[] p : spots) {
            int fx = 0;
            int fz = 0;
            if (Math.abs(p[0]) >= Math.abs(p[1])) fx = p[0] > 0 ? -1 : 1;
            else fz = p[1] > 0 ? -1 : 1;
            int hx = cx + p[0];
            int hz = cz + p[1];
            house(w, hx, sy, hz, fx, fz, mats, race);
            folk(new Location(w, hx + 0.5, sy + 1, hz + 0.5), race);
        }
        for (int i = 0; i < 3; i++) folk(spot(w, cx, cz, 3, sy + 1), race);
        for (int i = 0; i < 2; i++) soldier(spot(w, cx, cz, 3, sy + 1));
    }

    private void house(World w, int x, int sy, int z, int fx, int fz, Material[] m, String race) {
        Material wallM = m[0];
        Material pillar = m[1];
        Material roof = m[2];
        Material floorM = m[3];

        fill(w, x - 3, sy - 3, z - 3, x + 3, sy - 1, z + 3, Material.COBBLESTONE);
        fill(w, x - 3, sy, z - 3, x + 3, sy + 7, z + 3, Material.AIR);
        fill(w, x - 2, sy, z - 2, x + 2, sy, z + 2, floorM);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (!edge) continue;
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                for (int y = sy + 1; y <= sy + 3; y++) {
                    set(w, x + dx, y, z + dz, corner ? pillar : wallM);
                }
            }
        }
        fill(w, x - 3, sy + 4, z - 3, x + 3, sy + 4, z + 3, roof);
        fill(w, x - 2, sy + 5, z - 2, x + 2, sy + 5, z + 2, roof);
        fill(w, x - 1, sy + 6, z - 1, x + 1, sy + 6, z + 1, roof);

        // kapi ve pencereler
        set(w, x + 2 * fx, sy + 1, z + 2 * fz, Material.AIR);
        set(w, x + 2 * fx, sy + 2, z + 2 * fz, Material.AIR);
        int[][] wins = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] d : wins) {
            if (d[0] == 2 * fx && d[1] == 2 * fz) continue;
            set(w, x + d[0], sy + 2, z + d[1], Material.GLASS_PANE);
        }

        // esyalar (kapinin karsisinda)
        int bx = x - fx;
        int bz = z - fz;
        int lx = bx + Math.abs(fz);
        int lz = bz + Math.abs(fx);
        int rx = bx - Math.abs(fz);
        int rz = bz - Math.abs(fx);
        Material back;
        switch (race) {
            case "dwarf":
                back = Material.BLAST_FURNACE;
                break;
            case "elf":
                back = Material.BOOKSHELF;
                break;
            default:
                back = Material.CRAFTING_TABLE;
                break;
        }
        set(w, bx, sy + 1, bz, back);
        set(w, bx, sy + 2, bz, Material.TORCH);
        set(w, lx, sy + 1, lz, Material.BARREL);
        chest(w, rx, sy + 1, rz, 0);
    }

    // --------------------------------------------------------------- magara

    private Location floorSpot(World w, int cx, int cz, int cy, int half) {
        for (int t = 0; t < 40; t++) {
            int x = cx - half + rnd.nextInt(half * 2 + 1);
            int z = cz - half + rnd.nextInt(half * 2 + 1);
            for (int y = cy + 3; y >= cy - 4; y--) {
                if (free(w, x, y, z) && !w.getBlockAt(x, y - 1, z).getType().isAir()
                        && !w.getBlockAt(x, y - 1, z).isLiquid()) {
                    return new Location(w, x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }

    private boolean lair(Location base) {
        World w = base.getWorld();
        int x0 = base.getBlockX();
        int z0 = base.getBlockZ();
        int y0 = base.getBlockY();
        int len = Math.min(28, (y0 - (w.getMinHeight() + 12)) * 2);
        if (len < 10) return false;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] d = dirs[rnd.nextInt(4)];
        for (int i = 0; i <= len; i++) {
            int x = x0 + d[0] * i;
            int z = z0 + d[1] * i;
            int y = y0 - i / 2;
            for (int p = -1; p <= 1; p++) {
                int px = x + (d[0] == 0 ? p : 0);
                int pz = z + (d[1] == 0 ? p : 0);
                for (int h = 0; h <= 2; h++) set(w, px, y + h, pz, Material.AIR);
            }
        }
        int cx = x0 + d[0] * len;
        int cz = z0 + d[1] * len;
        int cy = y0 - len / 2 + 2;

        for (int dx = -7; dx <= 7; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -7; dz <= 7; dz++) {
                    if (dx * dx / 49.0 + dy * dy / 16.0 + dz * dz / 49.0 <= 1.0) {
                        set(w, cx + dx, cy + dy, cz + dz, Material.AIR);
                    }
                }
            }
        }
        // suslemeler: orumcek agi ve kemikler
        for (int t = 0; t < 14; t++) {
            int x = cx - 5 + rnd.nextInt(11);
            int z = cz - 5 + rnd.nextInt(11);
            for (int y = cy + 3; y >= cy - 4; y--) {
                Block b = w.getBlockAt(x, y, z);
                if (b.getType().isAir() && !w.getBlockAt(x, y - 1, z).getType().isAir()) {
                    b.setType(Material.BONE_BLOCK, false);
                    break;
                }
            }
            for (int y = cy - 3; y <= cy + 4; y++) {
                Block b = w.getBlockAt(x, y, z);
                if (b.getType().isAir() && !w.getBlockAt(x, y + 1, z).getType().isAir()) {
                    b.setType(Material.COBWEB, false);
                    break;
                }
            }
        }
        // canavarlar
        int wolves = 3 + rnd.nextInt(2);
        int bears = 1 + rnd.nextInt(2);
        for (int i = 0; i < wolves; i++) spawnHostile("kurt_adam", floorSpot(w, cx, cz, cy, 5), 2);
        for (int i = 0; i < bears; i++) spawnHostile("ayi", floorSpot(w, cx, cz, cy, 5), 2);

        // hazine sandigi (Thobredstone garantili)
        Location cs = floorSpot(w, cx, cz, cy, 2);
        if (cs != null) {
            Block b = cs.getBlock();
            b.setType(Material.CHEST, false);
            if (b.getState() instanceof Chest c) {
                Dungeons.fillLoot(c.getBlockInventory(), 3, rnd);
                ItemStack t = Items.make("thobredstone_ingot");
                t.setAmount(2 + rnd.nextInt(3));
                c.getBlockInventory().setItem(rnd.nextInt(27), t);
            }
        }
        return true;
    }

    // -------------------------------------------------- seytan bolgeleri

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

    private boolean isRegionCenter(int chunkX, int chunkZ) {
        return Math.floorMod(chunkX, 32) == 16 && Math.floorMod(chunkZ, 32) == 16;
    }

    private void corrupt(World w, int chunkX, int chunkZ) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkX * 16 + dx;
                int z = chunkZ * 16 + dz;
                int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                Block b = w.getBlockAt(x, y, z);
                Material t = b.getType();
                if (t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.COARSE_DIRT
                        || t == Material.PODZOL || t == Material.ROOTED_DIRT || t == Material.SAND
                        || t == Material.RED_SAND || t == Material.MYCELIUM) {
                    int r = rnd.nextInt(100);
                    b.setType(r < 50 ? Material.NETHERRACK : (r < 70 ? Material.BLACKSTONE : Material.SOUL_SOIL), false);
                }
            }
        }
    }

    // ------------------------------------------------------ dogal olusma

    private boolean badBiome(String biome) {
        return biome.contains("ocean") || biome.contains("river") || biome.contains("beach")
                || biome.contains("swamp");
    }

    private String raceFor(String biome) {
        if (biome.contains("peaks") || biome.contains("windswept") || biome.contains("hills")
                || biome.contains("badlands")) {
            return rnd.nextInt(10) < 8 ? "dwarf" : "human";
        }
        if (biome.contains("forest") || biome.contains("taiga") || biome.contains("grove")
                || biome.contains("cherry")) {
            return rnd.nextInt(10) < 6 ? "elf" : "human";
        }
        return "human";
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        World w = e.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return;

        int chunkX = e.getChunk().getX();
        int chunkZ = e.getChunk().getZ();
        boolean demon = isDemonRegion(w, chunkX, chunkZ);
        boolean center = demon && isRegionCenter(chunkX, chunkZ);
        int roll = rnd.nextInt(1000);
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;

        if (!demon && roll >= 16) return;
        if (demon && !center && roll >= 90) {
            Bukkit.getScheduler().runTask(plugin, () -> corrupt(w, chunkX, chunkZ));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (w.getBlockAt(x, y, z).isLiquid()) return;
            String biome = w.getBiome(x, y, z).getKey().getKey();
            Location base = new Location(w, x, y, z);

            if (demon) {
                corrupt(w, chunkX, chunkZ);
                if (center) {
                    castle(base, true);
                } else {
                    int n = 2 + rnd.nextInt(3);
                    for (int i = 0; i < n; i++) {
                        int px = x - 4 + rnd.nextInt(9);
                        int pz = z - 4 + rnd.nextInt(9);
                        int py = w.getHighestBlockYAt(px, pz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                        plugin.mobs().spawn("seytan", new Location(w, px + 0.5, py + 1, pz + 0.5));
                    }
                }
                return;
            }

            if (roll < 7) {
                if (badBiome(biome)) return;
                village(base, raceFor(biome));
            } else if (roll < 9) {
                if (badBiome(biome)) return;
                tower(base, 4 + rnd.nextInt(4));
            } else if (roll < 11) {
                if (badBiome(biome)) return;
                castle(base, false);
            } else {
                if (badBiome(biome)) return;
                lair(base);
            }
        });
    }
}
