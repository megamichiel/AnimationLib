package me.megamichiel.animationlib.config.type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import me.megamichiel.animationlib.LazyValue;
import me.megamichiel.animationlib.config.MapConfig;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GsonConfig extends MapConfig {

    private static final long serialVersionUID = 9171186229958126150L;

    private final transient Supplier<Gson> gson = LazyValue.of(() -> new GsonBuilder().setPrettyPrinting().create());
    private String indent = "    ";

    public GsonConfig() {
        this(true);
    }

    public GsonConfig(boolean caseInsensitive) {
        super(caseInsensitive);
    }

    @Override
    public void loadFromString(String gson) {
        super.loadFromString(gson);
        setAll(this.gson.get().fromJson(gson, HashMap.class));
    }

    @Override
    public String saveToString() {
        StringWriter string = new StringWriter();
        JsonWriter writer = new JsonWriter(string);
        writer.setIndent(indent);

        gson.get().toJson(toRawMap(), Map.class, writer);
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
