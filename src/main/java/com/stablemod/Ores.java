package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ores implements Listener {
    private static class Def {
        final String itemId;
        final int yMin, yMax, chance, vein, minTier;
        final Material stone, deep;

        Def(String itemId, int yMin, int yMax, int chance, int vein, int minTier,
            Material stone, Material deep) {
            this.itemId = itemId;
            this.yMin = yMin;
            this.yMax = yMax;
            this.chance = chance;
            this.vein = vein;
            this.minTier = minTier;
            this.stone = stone;
            this.deep = deep;
        }
    }

    private final List<Def> defs = new ArrayList<>();
    private final NamespacedKey key;
    private final Random rnd = new Random();

    public Ores(StableMod plugin) {
        key = new NamespacedKey(plugin, "ores");
        defs.add(new Def("mitril_ingot", -58, -30, 4, 4, 3, null, Material.DEEPSLATE_LAPIS_ORE));
        defs.add(new Def("darkirtel_ingot", -50, -5, 3, 3, 2, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE));
        defs.add(new Def("magceston_ingot", -30, 40, 2, 4, 2, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE));
        defs.add(new Def("oritegold_ingot", -30, 40, 3, 3, 2, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE));
        defs.add(new Def("solnarium_ingot", -50, 20, 6, 2, 3, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE));
        defs.add(new Def("dragonite_ingot", -64, -50, 6, 3, 3, null, Material.DEEPSLATE_REDSTONE_ORE));
    }

    private Def find(String itemId) {
        for (Def d : defs) {
            if (d.itemId.equals(itemId)) return d;
        }
        return null;
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        Chunk ch = e.getChunk();
        if (ch.getWorld().getEnvironment() != World.Environment.NORMAL) return;

        PersistentDataContainer pdc = ch.getPersistentDataContainer();
        StringBuilder sb = new StringBuilder(pdc.getOrDefault(key, PersistentDataType.STRING, ";"));
        boolean placed = false;

        for (Def d : defs) {
            if (rnd.nextInt(d.chance) != 0) continue;
            int bx = 1 + rnd.nextInt(14);
            int bz = 1 + rnd.nextInt(14);
            int by = d.yMin + rnd.nextInt(d.yMax - d.yMin + 1);
            for (int i = 0; i < d.vein; i++) {
                int x = bx + rnd.nextInt(3) - 1;
                int y = by + rnd.nextInt(3) - 1;
                int z = bz + rnd.nextInt(3) - 1;
                Block b = ch.getBlock(x, y, z);
                Material to = null;
                if (b.getType() == Material.STONE) to = d.stone;
                else if (b.getType() == Material.DEEPSLATE) to = d.deep;
                if (to == null) continue;
                b.setType(to, false);
                sb.append(b.getX()).append(",").append(b.getY()).append(",").append(b.getZ())
                        .append(":").append(d.itemId).append(";");
                placed = true;
            }
        }
        if (placed) pdc.set(key, PersistentDataType.STRING, sb.toString());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        String data = pdc.getOrDefault(key, PersistentDataType.STRING, "");
        if (data.isEmpty()) return;

        String search = ";" + b.getX() + "," + b.getY() + "," + b.getZ() + ":";
        int index = data.indexOf(search);
        if (index < 0) return;
        int end = data.indexOf(';', index + 1);
        if (end < 0) return;
        String itemId = data.substring(index + search.length(), end);
        Def d = find(itemId);

        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();

        if (p.getGameMode() == GameMode.SURVIVAL && d != null) {
            if (!tool.getType().name().endsWith("_PICKAXE") || Items.toolTier(tool) < d.minTier) {
                e.setCancelled(true);
                p.sendMessage(Component.text(
                        "Bu cevheri kırmak için daha güçlü bir kazma gerekiyor.", NamedTextColor.RED));
                return;
            }
        }

        pdc.set(key, PersistentDataType.STRING,
                data.substring(0, index + 1) + data.substring(end + 1));
        e.setDropItems(false);
        e.setExpToDrop(0);

        if (p.getGameMode() == GameMode.SURVIVAL) {
            ItemStack drop = Items.make(itemId);
            if (drop != null) {
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), drop);
            }
        }
    }
}
