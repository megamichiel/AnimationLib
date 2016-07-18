package me.megamichiel.animationlib.config;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GsonConfig extends MapConfig {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JsonParser parser = new JsonParser();
    private int indent = 2;

    public GsonConfig() {
        this(true);
    }

    public GsonConfig(boolean caseInsensitive) {
        super(caseInsensitive);
    }

    @Override
    public GsonConfig loadFromString(String gson) {
        super.loadFromString(gson);
        JsonElement element = parser.parse(gson);
        if (element instanceof JsonObject) {
            JsonObject obj = element.getAsJsonObject();
            deserialize(map -> map, toMap(obj));
        }
        return this;
    }

    @Override
    public String saveToString() {
        StringWriter string = new StringWriter();
        JsonWriter writer = new JsonWriter(string);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < this.indent; i += 2)
            indent.append("  ");
        writer.setIndent(indent.toString());

        gson.toJson(toRawMap(), Map.class, writer);
        return string.toString();
    }

    private Map<String, Object> toMap(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object o = convert(entry.getValue());
            if (o != null) map.put(key, o);
        }
        return map;
    }

    private List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonElement ele : array) {
            Object o = convert(ele);
            if (o != null) list.add(o);
        }
        return list;
    }

    private Object convert(JsonElement ele) {
        if (ele.isJsonObject()) return toMap(ele.getAsJsonObject());
        if (ele.isJsonArray()) return toList(ele.getAsJsonArray());
        if (ele.isJsonPrimitive()) {
            JsonPrimitive pr = ele.getAsJsonPrimitive();
            if (pr.isBoolean()) return pr.getAsBoolean();
            if (pr.isNumber())  return pr.getAsNumber();
            if (pr.isString())  return pr.getAsString();
        }
        return null;
    }

    @Override
    public void setIndent(int indent) {
        super.setIndent(indent);
        this.indent = indent;
    }
}
