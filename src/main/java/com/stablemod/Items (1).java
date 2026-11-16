package com.stablemod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Items {
    public static final List<String> PARTS = List.of(
            "ingot", "sword", "pickaxe", "axe", "helmet", "chestplate", "leggings", "boots");

    private static StableMod plugin;
    private static NamespacedKey idKey;

    public static void init(StableMod p) {
        plugin = p;
        idKey = new NamespacedKey(p, "id");
    }

    public static List<String> allIds() {
        List<String> out = new ArrayList<>();
        for (Minerals.M m : Minerals.ALL) {
            for (String part : PARTS) out.add(m.id + "_" + part);
        }
        return out;
    }

    public static String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public static int toolTier(ItemStack it) {
        String id = idOf(it);
        if (id != null) {
            Minerals.M m = Minerals.ofItemId(id);
            if (m != null) return m.tier;
        }
        String n = it.getType().name();
        if (n.startsWith("NETHERITE")) return 4;
        if (n.startsWith("DIAMOND")) return 3;
        if (n.startsWith("IRON")) return 2;
        if (n.startsWith("STONE")) return 1;
        return 0;
    }

    public static ItemStack make(String itemId) {
        Minerals.M m = Minerals.ofItemId(itemId);
        if (m == null) return null;
        String part = itemId.substring(m.id.length() + 1);
        switch (part) {
            case "ingot":
                return base(m.ingot, itemId, m.ingotName, m, "");
            case "sword":
                return tool(m, itemId, m.name + " Kılıç", "SWORD", m.sword, -2.4);
            case "pickaxe":
                return tool(m, itemId, m.name + " Kazma", "PICKAXE", m.pick, -2.8);
            case "axe":
                return tool(m, itemId, m.name + " Balta", "AXE", m.axe, -3.0);
            case "helmet":
                return armor(m, itemId, m.name + " Kask", "HELMET", 11, m.armor[0], EquipmentSlotGroup.HEAD);
            case "chestplate":
                return armor(m, itemId, m.name + " Göğüslük", "CHESTPLATE", 16, m.armor[1], EquipmentSlotGroup.CHEST);
            case "leggings":
                return armor(m, itemId, m.name + " Pantolon", "LEGGINGS", 15, m.armor[2], EquipmentSlotGroup.LEGS);
            case "boots":
                return armor(m, itemId, m.name + " Bot", "BOOTS", 13, m.armor[3], EquipmentSlotGroup.FEET);
            default:
                return null;
        }
    }

    private static ItemStack base(Material mat, String id, String name, Minerals.M m, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, m.color).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack tool(Minerals.M m, String id, String name, String type, double dmg, double spd) {
        Material mat = Material.valueOf(m.toolPrefix + "_" + type);
        ItemStack it = base(mat, id, name, m, m.toolLore);
        ItemMeta meta = it.getItemMeta();
        ((Damageable) meta).setMaxDamage(m.toolDur);
        attr(meta, Attribute.ATTACK_DAMAGE, dmg, EquipmentSlotGroup.MAINHAND, id + "_dmg");
        attr(meta, Attribute.ATTACK_SPEED, spd, EquipmentSlotGroup.MAINHAND, id + "_spd");
        if (m.enchantable > 0) meta.setEnchantable(m.enchantable);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack armor(Minerals.M m, String id, String name, String type, int durBase,
                                   double armor, EquipmentSlotGroup g) {
        Material mat = Material.valueOf(m.armorPrefix + "_" + type);
        ItemStack it = base(mat, id, name, m, m.armorLore);
        ItemMeta meta = it.getItemMeta();
        ((Damageable) meta).setMaxDamage(durBase * m.armorMult);
        attr(meta, Attribute.ARMOR, armor, g, id + "_arm");
        if (m.toughness > 0) attr(meta, Attribute.ARMOR_TOUGHNESS, m.toughness, g, id + "_tough");
        if (m.kb > 0) attr(meta, Attribute.KNOCKBACK_RESISTANCE, m.kb, g, id + "_kb");
        if (m.enchantable > 0) meta.setEnchantable(m.enchantable);
        it.setItemMeta(meta);
        return it;
    }

    private static void attr(ItemMeta meta, Attribute a, double v, EquipmentSlotGroup g, String n) {
        meta.addAttributeModifier(a, new AttributeModifier(
                new NamespacedKey(plugin, n), v, AttributeModifier.Operation.ADD_NUMBER, g));
    }
}
