package me.megamichiel.animationlib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class GsonConfig extends MapConfig {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private String indent = "    ";

    public GsonConfig() {
        this(true);
    }

    public GsonConfig(boolean caseInsensitive) {
        super(caseInsensitive);
    }

    @Override
    public GsonConfig loadFromString(String gson) {
        super.loadFromString(gson);
        deserialize(map -> map, this.gson.fromJson(gson, HashMap.class));
        return this;
    }

    @Override
    public String saveToString() {
        StringWriter string = new StringWriter();
        JsonWriter writer = new JsonWriter(string);
        writer.setIndent(indent);

        gson.toJson(toRawMap(), Map.class, writer);
        return string.toString();
    }

    @Override
    public void setIndent(int indent) {
        super.setIndent(indent);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i += 2) sb.append("  ");
        this.indent = sb.toString();
    }
}
