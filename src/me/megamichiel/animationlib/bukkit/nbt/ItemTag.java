package me.megamichiel.animationlib.bukkit.nbt;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class ItemTag implements ItemMeta, Cloneable {

    private final NBTTag tag;

    public ItemTag() {
        tag = new NBTTag(NBTUtil.getInstance().createTag());
    }

    public ItemTag(Object tag) {
        this.tag = new NBTTag(tag);
    }

    public ItemTag(ItemStack stack) throws IllegalArgumentException {
        NBTUtil nbt = NBTUtil.getInstance();
        if (!nbt.isSupported(stack)) throw new IllegalArgumentException("Not a CraftItemStack!");
        tag = new NBTTag(nbt.getOrCreateTag(stack));
    }

    public NBTTag getTag() {
        return tag;
    }

    public ItemStack toItemStack(ItemStack base) {
        NBTUtil nbt = NBTUtil.getInstance();
        base = nbt.asNMS(base);
        nbt.setTag(base, tag.getHandle());
        return base;
    }

    public ItemStack toItemStack(Material type) {
        return toItemStack(new ItemStack(type));
    }

    @Override
    public boolean hasDisplayName() {
        NBTTag display = tag.getTag("display");
        return display != null && display.contains("Name");
    }

    @Override
    public String getDisplayName() {
        NBTTag display = tag.getTag("display");
        return display == null ? null : display.getString("Name");
    }

    @Override
    public void setDisplayName(String s) {
        tag.getOrCreateTag("display").set("Name", s);
    }

    @Override
    public boolean hasLore() {
        NBTTag display = tag.getTag("display");
        return display != null && display.contains("Lore");
    }

    @Override
    public List<String> getLore() {
        NBTTag display = tag.getTag("display");
        if (display == null) return null;
        NBTList lore = tag.getList("Lore");
        if (lore == null) return null;
        NBTUtil.Modifier<String> modifier = NBTUtil.getInstance().modifier(String.class);
        return lore.getList().stream().filter(modifier::isInstance)
                .map(modifier::unwrap).collect(Collectors.toList());
    }

    @Override
    public void setLore(List<String> list) {
        NBTUtil.Modifier<String> modifier = NBTUtil.getInstance().modifier(String.class);
        tag.getOrCreateTag("display").set("Lore", list.stream().map(modifier::wrap).collect(Collectors.toList()));
    }

    public boolean hasLeatherColor() {
        NBTTag display = tag.getTag("display");
        return display != null && display.contains("color");
    }

    private static final Color DEFAULT_LEATHER_COLOR = Color.fromRGB(0xA06540);

    public Color getLeatherColor() {
        NBTTag display = tag.getTag("display");
        if (display == null) return DEFAULT_LEATHER_COLOR;
        return Color.fromRGB(display.getInt("color", 0xA06540));
    }

    public boolean isUnbreakable() {
        return tag.getBoolean("Unbreakable");
    }

    public void setUnbreakable(boolean unbreakable) {
        tag.set("Unbreakable", unbreakable);
    }

    public boolean hasSkullOwner() {
        return tag.contains("SkullOwner");
    }

    public String getSkullOwner() {
        String s = tag.getString("SkullOwner");
        if (s != null) return s;
        NBTTag owner = tag.getTag("SkullOwner");
        return owner == null ? null : owner.getString("Name");
    }

    public void setSkullOwner(String owner) {
        tag.set("SkullOwner", owner);
    }

    @Override
    public boolean hasEnchants() {
        NBTList list = tag.getList("ench");
        return list != null && !list.isEmpty();
    }

    @Override
    public boolean hasEnchant(Enchantment enchantment) {
        NBTList list = tag.getList("ench");
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            if ((tag.getShort("id") & 0xFFFF) == enchantment.getId())
                return true;
        }
        return false;
    }

    @Override
    public int getEnchantLevel(Enchantment enchantment) {
        NBTList list = tag.getList("ench");
        if (list == null) return 0;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            if ((tag.getShort("id") & 0xFFFF) == enchantment.getId())
                return tag.getShort("lvl") & 0xFFFF;
        }
        return 0;
    }

    @Override
    public Map<Enchantment, Integer> getEnchants() {
        NBTList list = tag.getList("ench");
        if (list == null) return Collections.emptyMap();
        Map<Enchantment, Integer> ench = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            Enchantment e = Enchantment.getById(tag.getShort("id") & 0xFFFF);
            if (e == null) continue;
            int lvl = tag.getShort("lvl") & 0xFFFF;
            if (lvl != 0) ench.put(e, lvl);
        }
        return ench;
    }

    @Override
    public boolean addEnchant(Enchantment ench, int lvl, boolean ignoreRestrictions) {
        NBTList list = tag.getList("ench");
        if (list == null) list = tag.createList("ench");
        if (!ignoreRestrictions && (lvl < ench.getStartLevel() || lvl > ench.getMaxLevel()))
            return false;
        short slvl = (short) lvl;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            if ((tag.getShort("id") & 0xFFFF) == ench.getId()) {
                if (tag.getShort("lvl") == slvl) return false;
                tag.set("lvl", slvl);
                return true;
            }
        }
        NBTTag tag = new NBTTag();
        tag.set("id", (short) ench.getId());
        tag.set("lvl", slvl);
        list.addRaw(tag.getHandle());
        return true;
    }

    @Override
    public boolean removeEnchant(Enchantment enchantment) {
        NBTList list = tag.getList("ench");
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            if ((tag.getShort("id") & 0xFFFF) == enchantment.getId()) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasConflictingEnchant(Enchantment enchantment) {
        NBTList list = tag.getList("ench");
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            Enchantment ench = Enchantment.getById(tag.getShort("id") & 0xFFFF);
            if (ench != null && enchantment.conflictsWith(ench))
                return true;
        }
        return false;
    }

    @Override
    public void addItemFlags(ItemFlag... flags) {
        int i = tag.getInt("HideFlags");
        for (ItemFlag flag : flags) i |= 1 << flag.ordinal();
        tag.set("HideFlags", i);
    }

    @Override
    public void removeItemFlags(ItemFlag... flags) {
        int i = tag.getInt("HideFlags");
        for (ItemFlag flag : flags) i &= ~(1 << flag.ordinal());
        if (i == 0) tag.remove("HideFlags");
        else tag.set("HideFlags", i);
    }

    @Override
    public Set<ItemFlag> getItemFlags() {
        int i = tag.getInt("HideFlags");
        Set<ItemFlag> flags = new HashSet<>();
        for (int j = 0; j < ItemFlag.values().length; j++)
            if ((i & (1 << j)) != 0) flags.add(ItemFlag.values()[j]);
        return flags;
    }

    @Override
    public boolean hasItemFlag(ItemFlag flag) {
        return (tag.getInt("HideFlags") & (1 << flag.ordinal())) != 0;
    }

    public Set<String> getCanDestroy() {
        NBTList list = tag.getList("CanDestroy");
        if (list == null) return Collections.emptySet();
        NBTUtil.Modifier<String> mod = NBTUtil.getInstance().modifier(String.class);
        return list.getList().stream().filter(mod::isInstance)
                .map(mod::unwrap).collect(Collectors.toSet());
    }

    public void setCanDestroy(Set<String> canDestroy) {
        NBTUtil.Modifier<String> mod = NBTUtil.getInstance().modifier(String.class);
        tag.set("CanDestroy", canDestroy.stream().map(mod::wrap).collect(Collectors.toSet()));
    }

    public Set<String> getCanPlaceOn() {
        NBTList list = tag.getList("CanPlaceOn");
        if (list == null) return Collections.emptySet();
        NBTUtil.Modifier<String> mod = NBTUtil.getInstance().modifier(String.class);
        return list.getList().stream().filter(mod::isInstance)
                .map(mod::unwrap).collect(Collectors.toSet());
    }

    public void setCanPlaceOn(Set<String> canPlaceOn) {
        NBTUtil.Modifier<String> mod = NBTUtil.getInstance().modifier(String.class);
        tag.set("CanPlaceOn", canPlaceOn.stream().map(mod::wrap).collect(Collectors.toSet()));
    }

    public EntityType getSpawnEggType() {
        NBTTag entity = tag.getTag("EntityTag");
        if (entity == null) return null;
        String id = entity.getString("id");
        return id == null ? null : EntityType.fromName(id);
    }

    public void setSpawnEggType(EntityType type) {
        tag.getOrCreateTag("EntityTag").set("id", type.getName());
    }

    public DyeColor getBaseColor() {
        NBTTag entity = tag.getTag("BlockEntityTag");
        if (entity == null) return null;
        return DyeColor.getByDyeData((byte) entity.getInt("Base"));
    }

    public void setBaseColor(DyeColor color) {
        tag.getOrCreateTag("BlockEntityTag").set("Base", color.getDyeData() & 0xFF);
    }

    public List<Pattern> getPatterns() {
        NBTTag entity = tag.getTag("BlockEntityTag");
        if (entity == null) return Collections.emptyList();
        NBTList patterns = entity.getList("Patterns");
        if (patterns == null) return Collections.emptyList();
        List<Pattern> list = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            NBTTag tag = patterns.getTag(i);
            DyeColor color = DyeColor.getByDyeData((byte) tag.getInt("Color"));
            PatternType type = PatternType.getByIdentifier(tag.getString("Pattern"));
            if (color != null && type != null) list.add(new Pattern(color, type));
        }
        return list;
    }

    public void setPatterns(List<Pattern> patterns) {
        NBTList list = tag.getOrCreateTag("BlockEntityTag").getOrCreateList("Patterns");
        list.clear();
        for (Pattern pattern : patterns) {
            NBTTag tag = new NBTTag();
            tag.set("Color", pattern.getColor().getDyeData() & 0xFF);
            tag.set("Pattern", pattern.getPattern().getIdentifier());
            list.addRaw(tag.getHandle());
        }
    }

    @Override
    public ItemTag clone() {
        try {
            return (ItemTag) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        throw new UnsupportedOperationException("Not implemented (yet?!?!?!)!");
    }

    public NBTTag getHandle() {
        return tag;
    }
}
