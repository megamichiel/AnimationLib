package me.megamichiel.animationlib.config.type;

import me.megamichiel.animationlib.config.MapConfig;

import java.io.*;
import java.util.Base64;

public class Base64Config extends MapConfig {

    @Override
    public String saveToString() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(bytes).writeObject(this);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public Base64Config loadFromString(String dump) {
        try {
            byte[] data = Base64.getDecoder().decode(dump);
            return (Base64Config) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (Exception ex) {
            return this;
        }
    }
}
