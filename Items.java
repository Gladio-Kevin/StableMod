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

public class Items {
    private static StableMod plugin;

    public static void init(StableMod p) {
        plugin = p;
    }

    public static ItemStack make(String id) {
        switch (id) {
            case "mitril_ingot":
                return base(Material.ECHO_SHARD, id, "Mitril Külçesi");
            case "mitril_sword":
                return tool(Material.NETHERITE_SWORD, id, "Mitril Kılıç", 3000, 8.5, -2.4);
            case "mitril_pickaxe":
                return tool(Material.NETHERITE_PICKAXE, id, "Mitril Kazma", 3000, 5.5, -2.8);
            case "mitril_axe":
                return tool(Material.NETHERITE_AXE, id, "Mitril Balta", 3000, 10.0, -3.0);
            case "mitril_helmet":
                return armor(Material.NETHERITE_HELMET, id, "Mitril Kask", 610, 4, EquipmentSlotGroup.HEAD);
            case "mitril_chestplate":
                return armor(Material.NETHERITE_CHESTPLATE, id, "Mitril Göğüslük", 888, 9, EquipmentSlotGroup.CHEST);
            case "mitril_leggings":
                return armor(Material.NETHERITE_LEGGINGS, id, "Mitril Pantolon", 832, 7, EquipmentSlotGroup.LEGS);
            case "mitril_boots":
                return armor(Material.NETHERITE_BOOTS, id, "Mitril Bot", 721, 4, EquipmentSlotGroup.FEET);
            default:
                return null;
        }
    }

    private static ItemStack base(Material m, String id, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "id"), PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack tool(Material m, String id, String name, int dur, double dmg, double spd) {
        ItemStack it = base(m, id, name);
        ItemMeta meta = it.getItemMeta();
        ((Damageable) meta).setMaxDamage(dur);
        attr(meta, Attribute.ATTACK_DAMAGE, dmg, EquipmentSlotGroup.MAINHAND, id + "_dmg");
        attr(meta, Attribute.ATTACK_SPEED, spd, EquipmentSlotGroup.MAINHAND, id + "_spd");
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack armor(Material m, String id, String name, int dur, double armor, EquipmentSlotGroup g) {
        ItemStack it = base(m, id, name);
        ItemMeta meta = it.getItemMeta();
        ((Damageable) meta).setMaxDamage(dur);
        attr(meta, Attribute.ARMOR, armor, g, id + "_arm");
        attr(meta, Attribute.ARMOR_TOUGHNESS, 4.0, g, id + "_tough");
        attr(meta, Attribute.KNOCKBACK_RESISTANCE, 0.15, g, id + "_kb");
        it.setItemMeta(meta);
        return it;
    }

    private static void attr(ItemMeta meta, Attribute a, double v, EquipmentSlotGroup g, String n) {
        meta.addAttributeModifier(a, new AttributeModifier(
                new NamespacedKey(plugin, n), v, AttributeModifier.Operation.ADD_NUMBER, g));
    }
}
