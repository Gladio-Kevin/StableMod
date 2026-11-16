package com.stablemod;

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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class MitrilOre implements Listener {
    private final NamespacedKey key;
    private final Random rnd = new Random();

    public MitrilOre(StableMod plugin) {
        key = new NamespacedKey(plugin, "mitril_ore");
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        Chunk ch = e.getChunk();
        World w = ch.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return;
        if (rnd.nextInt(4) != 0) return;

        int bx = 1 + rnd.nextInt(14);
        int bz = 1 + rnd.nextInt(14);
        int by = -58 + rnd.nextInt(30);

        PersistentDataContainer pdc = ch.getPersistentDataContainer();
        String data = pdc.getOrDefault(key, PersistentDataType.STRING, "");
        StringBuilder sb = new StringBuilder(data.isEmpty() ? ";" : data);
        boolean placed = false;

        for (int i = 0; i < 4; i++) {
            int x = bx + rnd.nextInt(3) - 1;
            int y = by + rnd.nextInt(3) - 1;
            int z = bz + rnd.nextInt(3) - 1;
            Block b = ch.getBlock(x, y, z);
            if (b.getType() == Material.DEEPSLATE) {
                b.setType(Material.DEEPSLATE_LAPIS_ORE, false);
                sb.append(b.getX()).append(",").append(b.getY()).append(",").append(b.getZ()).append(";");
                placed = true;
            }
        }
        if (placed) pdc.set(key, PersistentDataType.STRING, sb.toString());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.DEEPSLATE_LAPIS_ORE) return;

        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        String data = pdc.getOrDefault(key, PersistentDataType.STRING, "");
        String search = ";" + b.getX() + "," + b.getY() + "," + b.getZ() + ";";
        if (!data.contains(search)) return;

        pdc.set(key, PersistentDataType.STRING, data.replace(search, ";"));
        e.setDropItems(false);
        e.setExpToDrop(0);

        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SURVIVAL
                && b.isPreferredTool(p.getInventory().getItemInMainHand())) {
            b.getWorld().dropItemNaturally(
                    b.getLocation().add(0.5, 0.5, 0.5), Items.make("mitril_ingot"));
        }
    }
}
