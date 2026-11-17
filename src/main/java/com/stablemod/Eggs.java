package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Eggs implements Listener {
    private static final Map<String, Material> MATS = new LinkedHashMap<>();
    private static final Map<String, String> NAMES = new LinkedHashMap<>();
    private static NamespacedKey eggKey;

    static {
        add("ulu_kurt", Material.WOLF_SPAWN_EGG, "Ulu Kurt");
        add("olu_fisildayan", Material.STRAY_SPAWN_EGG, "Ölü Fısıldayan");
        add("orman_cadisi", Material.WITCH_SPAWN_EGG, "Orman Cadısı");
        add("dev", Material.ZOMBIE_SPAWN_EGG, "Dev");
        add("goblin", Material.ZOMBIE_VILLAGER_SPAWN_EGG, "Goblin");
        add("hoggoblin", Material.HOGLIN_SPAWN_EGG, "Hoggoblin");
        add("kurt_adam", Material.HUSK_SPAWN_EGG, "Kurt Adam");
        add("ayi", Material.POLAR_BEAR_SPAWN_EGG, "Ayı");
        add("seytan", Material.PIGLIN_BRUTE_SPAWN_EGG, "Şeytan");
        add("seytan_kral", Material.WITHER_SKELETON_SPAWN_EGG, "Şeytan Kral");
        add("asker", Material.VINDICATOR_SPAWN_EGG, "Kale Askeri");
        add("insan_koylu", Material.VILLAGER_SPAWN_EGG, "İnsan Köylü");
        add("cuce", Material.WANDERING_TRADER_SPAWN_EGG, "Cüce");
        add("elf_koylu", Material.ALLAY_SPAWN_EGG, "Elf");
    }

    private static void add(String id, Material m, String name) {
        MATS.put(id, m);
        NAMES.put(id, name);
    }

    private final StableMod plugin;

    public Eggs(StableMod plugin) {
        this.plugin = plugin;
        eggKey = new NamespacedKey(plugin, "egg");
    }

    public static List<String> allIds() {
        List<String> out = new ArrayList<>();
        for (String id : MATS.keySet()) out.add(id + "_egg");
        return out;
    }

    public static ItemStack make(String itemId) {
        if (!itemId.endsWith("_egg")) return null;
        String id = itemId.substring(0, itemId.length() - 4);
        Material m = MATS.get(id);
        if (m == null) return null;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(NAMES.get(id) + " Yumurtası", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(eggKey, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta() || e.getClickedBlock() == null) return;
        String id = it.getItemMeta().getPersistentDataContainer().get(eggKey, PersistentDataType.STRING);
        if (id == null) return;
        e.setCancelled(true);

        Location loc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        LivingEntity m = plugin.mobs().spawn(id, loc);
        if (m == null) m = plugin.structures().spawnNpc(id, loc);
        if (m != null && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            it.setAmount(it.getAmount() - 1);
        }
    }
}
