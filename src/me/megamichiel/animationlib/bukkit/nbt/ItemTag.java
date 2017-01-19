package me.megamichiel.animationlib.bukkit.nbt;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "unused", "WeakerAccess"})
public class ItemTag {

    private final NBTTag tag;

    public ItemTag() {
        tag = new NBTTag();
    }

    public ItemTag(Object tag) {
        this.tag = new NBTTag(tag);
    }

    public ItemTag(ItemStack stack) throws IllegalArgumentException {
        NBTUtil nbt = NBTUtil.getInstance();
        if (!nbt.isSupported(stack)) throw new IllegalArgumentException("Not a CraftItemStack!");
        tag = new NBTTag(nbt.getOrCreateTag(stack));
    }

    public NBTTag getHandle() {
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

    public ItemStack toItemStack(Material type, int amount) {
        return toItemStack(new ItemStack(type, amount));
    }

    public ItemStack toItemStack(Material type, int amount, short data) {
        return toItemStack(new ItemStack(type, amount, data));
    }

    public boolean hasDisplay() {
        NBTTag display = tag.getTag("display");
        return display != null && (display.contains("Name") || display.contains("Lore") || display.contains("color"));
    }

    public Display getDisplay() {
        NBTTag display = tag.getTag("display");
        if (display == null) return new Display();
        String name = display.getString("Name");
        NBTList loreList = display.getList("Lore");
        NBTUtil.Modifier<String> modifier = NBTUtil.getInstance().modifier(String.class);
        List<String> lore = loreList == null ? null : loreList.getList().stream()
                .filter(modifier::isInstance).map(modifier::unwrap).collect(Collectors.toList());
        Color color = display.contains("color") ? Color.fromRGB(display.getInt("color")) : null;
        return new Display(name, lore, color);
    }

    public void setDisplay(Display display) {
        if (display == null || !(display.hasName() || display.hasLore() || display.hasColor())) {
            this.tag.remove("display");
            return;
        }
        NBTTag tag = this.tag.getOrCreateTag("display");
        tag.set("Name", display.getName());
        NBTUtil.Modifier<String> mod = NBTUtil.getInstance().modifier(String.class);
        tag.set("Lore", display.hasLore() ? display.getLore().stream().map(mod::wrap).collect(Collectors.toList()) : null);
        tag.set("color", display.hasColor() ? display.getColor().asRGB() : null);
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

    public int getRepairCost() {
        return tag.getInt("RepairCost");
    }

    public void setRepairCost(int i) {
        tag.set("RepairCost", i == 0 ? null : i);
    }

    public Ench getEnchants() {
        NBTList list = tag.getList("ench");
        if (list == null) return null;
        Ench ench = new Ench();
        Map<Enchantment, Integer> handle = ench.handle;
        for (int i = 0; i < list.size(); i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null) continue;
            int id = tag.getShort("id") & 0xFFFF, lvl = tag.getShort("lvl") & 0xFFFF;
            Enchantment enchant = Enchantment.getById(id);
            if (enchant != null && lvl > 0) handle.put(enchant, lvl);
        }
        return ench;
    }

    public void setEnchants(Ench ench) {
        if (ench == null) tag.remove("ench");
        else {
            NBTList list = tag.createList("ench");
            ench.handle.forEach((enchant, lvl) -> {
                NBTTag tag = new NBTTag();
                tag.set("id", enchant.getId());
                tag.set("lvl", lvl);
                list.addRaw(tag.getHandle());
            });
        }
    }

    public ItemFlags getItemFlags() {
        return new ItemFlags(tag.getInt("HideFlags"));
    }

    public void setItemFlags(ItemFlags itemFlags) {
        tag.set("HideFlags", itemFlags == null || itemFlags.mask == 0 ? null : itemFlags.mask);
    }

    public void setItemFlags(int mask) {
        tag.set("HideFlags", mask == 0 ? null : mask);
    }

    public boolean hasAttributes() {
        NBTList list = tag.getList("AttributeModifiers");
        return list != null && !list.isEmpty();
    }

    public Attributes getAttributes() {
        Attributes attributes = new Attributes();
        NBTList list = tag.getList("AttributeModifiers");
        if (list == null) return attributes;
        AttributeOperation[] operations = AttributeOperation.values();
        for (int i = 0, size = list.size(); i < size; i++) {
            NBTTag tag = list.getTag(i);
            if (tag == null || !tag.contains("Operation") || !tag.contains("Amount")) continue;
            int operation = tag.getByte("Operation") & 0xFF;
            if (operation < 0 || operation >= operations.length) continue;
            String attrName = tag.getString("AttributeName"),
                    name = tag.getString("Name"), slot = tag.getString("Slot");
            if (name == null || slot == null) continue;
            if (attrName == null) attrName = name;
            double amount = tag.getDouble("Amount");
            UUID id;
            if (tag.contains("UUIDMost") && tag.contains("UUIDLeave"))
                id = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
            else id = UUID.randomUUID();
            ItemSlot itemSlot;
            try {
                itemSlot = ItemSlot.valueOf(slot.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            attributes.getAttribute(new Attribute(attrName)).addModifier(new AttributeModifier(itemSlot, id, name, amount, AttributeOperation.values()[operation]));
        }
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        if (attributes == null) tag.remove("AttributeModifiers");
        else {
            NBTList list = tag.createList("AttributeModifiers");
            for (AttributeInstance instance : attributes.instances.values()) {
                Map<UUID, AttributeModifier> modifiers = instance.modifiers;
                if (modifiers.isEmpty()) continue;
                String attr = instance.attribute.name;
                for (AttributeModifier modifier : modifiers.values()) {
                    NBTTag tag = new NBTTag();
                    String name = modifier.getName();
                    if (modifier.slot != null)
                        tag.set("Slot", modifier.slot.name().toLowerCase(Locale.ENGLISH));
                    if (!name.equals(attr)) tag.set("AttributeName", attr);
                    tag.set("Name", name);
                    tag.set("Operation", (byte) modifier.getOperation().ordinal());
                    tag.set("Amount", modifier.getAmount());
                    UUID id = modifier.getUniqueId();
                    tag.set("UUIDMost", id.getMostSignificantBits());
                    tag.set("UUIDLeast", id.getLeastSignificantBits());
                    list.addRaw(tag.getHandle());
                }
            }
            if (list.isEmpty()) tag.remove("AttributeModifiers");
        }
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

    public EntityType getEntityType() {
        NBTTag entity = tag.getTag("EntityTag");
        if (entity == null) return null;
        String id = entity.getString("id");
        return id == null ? null : EntityType.fromName(id);
    }

    public void setEntityType(EntityType type) {
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

    public int getGeneration() {
        return tag.getInt("generation");
    }

    public void setGeneration(int generation) {
        tag.set("generation", generation <= 0 ? null : Math.min(generation, 2));
    }

    @Override
    public ItemTag clone() {
        try {
            return (ItemTag) super.clone();
        } catch (CloneNotSupportedException ex) {
            return new ItemTag(tag.clone());
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ItemTag && ((ItemTag) obj).tag.equals(tag));
    }

    public static class Display {

        private static final Color DEFAULT_LEATHER_COLOR = Bukkit.getItemFactory().getDefaultLeatherColor();

        private String name;
        private List<String> lore;
        private Color color;

        public Display(String name, List<String> lore, Color color) {
            this.name = name;
            this.lore = lore == null ? Collections.emptyList() : lore;
            this.color = color;
        }

        public Display(String name, List<String> lore) {
            this(name, lore, null);
        }

        public Display(String name) {
            this(name, Collections.emptyList(), null);
        }

        public Display() {
            this(null, Collections.emptyList(), null);
        }

        public Display setName(String name) {
            this.name = name;
            return this;
        }

        public Display setLore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Display setColor(Color color) {
            this.color = color;
            return this;
        }

        public boolean hasName() {
            return name != null;
        }

        public String getName() {
            return name;
        }

        public boolean hasLore() {
            return !lore.isEmpty();
        }

        public List<String> getLore() {
            return lore;
        }

        public boolean hasColor() {
            return color != null && !color.equals(DEFAULT_LEATHER_COLOR);
        }

        public Color getColor() {
            return color;
        }
    }

    public static class Ench {

        private final Map<Enchantment, Integer> handle = new HashMap<>();

        public Ench(Map<Enchantment, Integer> map) {
            setAll(map);
        }

        public Ench() {}

        public Map<Enchantment, Integer> getHandle() {
            return handle;
        }

        public boolean has(Enchantment ench) {
            return handle.containsKey(ench);
        }

        public boolean conflictsWith(Enchantment ench) {
            for (Enchantment enchant : handle.keySet())
                if (enchant.conflictsWith(ench)) return true;
            return false;
        }

        public int get(Enchantment ench) {
            Integer level = handle.get(ench);
            return level == null ? 0 : level;
        }

        public Ench set(Enchantment ench, int lvl) {
            if (lvl == 0) handle.remove(ench);
            else handle.put(ench, lvl);
            return this;
        }

        public Ench remove(Enchantment ench) {
            handle.remove(ench);
            return this;
        }

        public Ench setAll(Map<Enchantment, Integer> map) {
            map.forEach(this::set);
            return this;
        }
    }

    public static class ItemFlags {

        private int mask;

        public ItemFlags(ItemFlag... flags) {
            for (ItemFlag flag : flags)
                mask |= 1 << flag.ordinal();
        }

        public ItemFlags(int mask) {
            this.mask = mask;
        }

        public ItemFlags() {}

        public void add(ItemFlag... flags) {
            for (ItemFlag flag : flags)
                mask |= 1 << flag.ordinal();
        }

        public void set(ItemFlag... flags) {
            mask = 0;
            add(flags);
        }

        public void remove(ItemFlag... flags) {
            int i = 0;
            for (ItemFlag flag : flags)
                i |= 1 << flag.ordinal();
            mask &= ~i;
        }

        public boolean has(ItemFlag flag) {
            return (mask & (1 << flag.ordinal())) != 0;
        }
    }

    public static class Attributes {

        private final Map<Attribute, AttributeInstance> instances = new HashMap<>();

        public AttributeInstance getAttribute(Attribute attribute) {
            return instances.computeIfAbsent(attribute, AttributeInstance::new);
        }
    }

    public static class AttributeInstance {

        private final Attribute attribute;
        private final Map<UUID, AttributeModifier> modifiers = new HashMap<>();

        private AttributeInstance(Attribute attribute) {
            this.attribute = attribute;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public Set<AttributeModifier> getModifiers() {
            return new HashSet<>(modifiers.values());
        }

        public void addModifier(AttributeModifier modifier) {
            if (modifiers.putIfAbsent(modifier.getUniqueId(), modifier) != null)
                throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        }

        public AttributeModifier getModifier(UUID uuid) {
            return modifiers.get(uuid);
        }

        public void removeModifier(AttributeModifier modifier) {
            modifiers.remove(modifier.getUniqueId());
        }
    }

    public enum ItemSlot {
        MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET
    }

    public static class AttributeModifier {

        private final ItemSlot slot;
        private final UUID uuid;
        private final String name;
        private final double amount;
        private final AttributeOperation operation;

        public AttributeModifier(ItemSlot slot, String name, double amount, AttributeOperation operation) {
            this(slot, UUID.randomUUID(), name, amount, operation);
        }

        public AttributeModifier(ItemSlot slot, UUID uuid, String name, double amount, AttributeOperation operation) {
            Validate.notNull(uuid, "uuid");
            Validate.notEmpty(name, "Name cannot be empty");
            Validate.notNull(operation, "operation");
            this.slot = slot;
            this.uuid = uuid;
            this.name = name;
            this.amount = amount;
            this.operation = operation;
        }

        public ItemSlot getSlot() {
            return slot;
        }

        public UUID getUniqueId() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public double getAmount() {
            return amount;
        }

        public AttributeOperation getOperation() {
            return operation;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof AttributeModifier && ((AttributeModifier) obj).getUniqueId().equals(getUniqueId()));
        }

        @Override
        public int hashCode() {
            return getUniqueId().hashCode();
        }
    }

    public enum AttributeOperation {
        ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1
    }

    public static class Attribute {

        public static final Attribute
                GENERIC_MAX_HEALTH = new Attribute("generic.maxHealth"),
                GENERIC_FOLLOW_RANGE = new Attribute("generic.followRange"),
                GENERIC_KNOCKBACK_RESISTANCE = new Attribute("generic.knockbackResistance"),
                GENERIC_MOVEMENT_SPEED = new Attribute("generic.movementSpeed"),
                GENERIC_ATTACK_DAMAGE = new Attribute("generic.attackDamage"),
                GENERIC_ATTACK_SPEED = new Attribute("generic.attackSpeed"),
                GENERIC_ARMOR = new Attribute("generic.armor"),
                GENERIC_ARMOR_TOUGHNESS = new Attribute("generic.armorToughness"),
                GENERIC_LUCK = new Attribute("generic.luck"),
                HORSE_JUMP_STRENGTH = new Attribute("horse.jumpStrength"),
                ZOMBIE_SPAWN_REINFORCEMENTS = new Attribute("zombie.spawnReinforcements");

        private final String name;

        public Attribute(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Attribute && ((Attribute) obj).name.equals(name));
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
