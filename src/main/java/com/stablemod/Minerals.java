package com.stablemod;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

public class Minerals {
    public static class M {
        public final String id, name, ingotName, toolPrefix, armorPrefix, toolLore, armorLore;
        public final NamedTextColor color;
        public final Material ingot;
        public final int tier, toolDur, armorMult, enchantable;
        public final double sword, pick, axe, toughness, kb;
        public final int[] armor;

        M(String id, String name, String ingotName, NamedTextColor color, Material ingot,
          String toolPrefix, String armorPrefix, int tier, int toolDur, int armorMult,
          double sword, double pick, double axe, int[] armor, double toughness, double kb,
          int enchantable, String toolLore, String armorLore) {
            this.id = id;
            this.name = name;
            this.ingotName = ingotName;
            this.color = color;
            this.ingot = ingot;
            this.toolPrefix = toolPrefix;
            this.armorPrefix = armorPrefix;
            this.tier = tier;
            this.toolDur = toolDur;
            this.armorMult = armorMult;
            this.sword = sword;
            this.pick = pick;
            this.axe = axe;
            this.armor = armor;
            this.toughness = toughness;
            this.kb = kb;
            this.enchantable = enchantable;
            this.toolLore = toolLore;
            this.armorLore = armorLore;
        }
    }

    public static final List<M> ALL = List.of(
        new M("mitril", "Mitril", "Mitril Külçesi", NamedTextColor.AQUA, Material.ECHO_SHARD,
            "NETHERITE", "NETHERITE", 4, 3000, 55, 8.5, 5.5, 10.0,
            new int[]{4, 9, 7, 4}, 4.0, 0.15, 0, "", ""),
        new M("darkirtel", "Darkirtel", "Darkirtel Külçesi", NamedTextColor.DARK_GRAY, Material.NETHERITE_SCRAP,
            "NETHERITE", "NETHERITE", 3, 1800, 33, 6.5, 4.5, 8.5,
            new int[]{3, 8, 6, 3}, 2.0, 0.05, 0, "Vurduğun rakibe 3 saniye körlük verir.", ""),
        new M("magceston", "Magceston", "Magceston Külçesi", NamedTextColor.YELLOW, Material.AMETHYST_SHARD,
            "IRON", "GOLDEN", 2, 200, 15, 5.5, 3.5, 7.5,
            new int[]{2, 6, 5, 2}, 0.0, 0.0, 40, "Büyü basınca çok daha güçlü olur.", "Büyü basınca çok daha güçlü olur."),
        new M("oritegold", "Oritegold", "Oritegold Külçesi", NamedTextColor.GOLD, Material.GOLD_INGOT,
            "IRON", "GOLDEN", 2, 300, 12, 6.5, 4.0, 8.5,
            new int[]{3, 8, 6, 3}, 1.0, 0.0, 0, "", ""),
        new M("solnarium", "Solnarium", "Solnarium Külçesi", NamedTextColor.WHITE, Material.GLOWSTONE_DUST,
            "DIAMOND", "DIAMOND", 3, 2200, 33, 6.5, 4.5, 8.5,
            new int[]{3, 8, 6, 3}, 2.0, 0.0, 0, "Karanlığı biraz aydınlatır.", "Karanlığı biraz aydınlatır."),
        new M("dragonite", "Dragonite", "Dragonite Külçesi", NamedTextColor.RED, Material.MAGMA_CREAM,
            "NETHERITE", "NETHERITE", 4, 2800, 52, 9.0, 6.0, 10.5,
            new int[]{4, 9, 7, 4}, 3.5, 0.1, 0, "", "Ateşe ve lava karşı korur. Tam sette bağışıklık verir."),
        new M("thobredstone", "Thobredstone", "Thobredstone Taşı", NamedTextColor.RED, Material.RED_DYE,
            "IRON", "IRON", 2, 250, 15, 6.0, 3.5, 8.5,
            new int[]{2, 6, 5, 2}, 0.0, 0.0, 0, "", "Tam sette +5 kalp verir.")
    );

    public static M ofItemId(String itemId) {
        for (M m : ALL) {
            if (itemId.startsWith(m.id + "_")) return m;
        }
        return null;
    }
}
