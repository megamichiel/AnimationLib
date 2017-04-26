package me.megamichiel.animationlib.bukkit.nbt;

public class NBTKey<T> {

    final String id;
    final NBTUtil.Modifier<T> modifier;

    NBTKey(String id, NBTUtil.Modifier<T> modifier) {
        this.id = id;
        this.modifier = modifier;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof NBTKey && ((NBTKey) obj).id.equals(id));
    }
}
