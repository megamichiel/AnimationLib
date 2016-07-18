package me.megamichiel.animationlib.config;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class MapConfig extends AbstractConfig implements Serializable {

    private static final Function<String, String> TO_LOWER = s -> s.toLowerCase(Locale.US);

    private final Map<String, Object> parent;
    private transient Function<String, String> keyMapper;

    public MapConfig(Map<?, ?> map) {
        this(map, true);
    }
    
    public MapConfig(Map<?, ?> map, boolean caseInsensitive) {
        keyMapper = caseInsensitive ? TO_LOWER : Function.identity();
        parent = mapValues(map);
    }

    private Object mapValue(Object o) {
        if (o instanceof Map) return new MapConfig(mapValues((Map) o));
        else if (o instanceof Iterable) return mapValues((Iterable) o);
        else if (o instanceof String) {
            String s = ((String) o).toLowerCase(Locale.US);
            switch (s) {
                case "true":  return Boolean.TRUE;
                case "false": return Boolean.FALSE;
                default:
                    try {
                        if (s.indexOf('.') != -1) return Double.parseDouble(s);
                        long l = Long.parseLong(s);
                        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                        return l;
                    } catch (NumberFormatException ex) {
                        return o;
                    }
            }
        } else if (o instanceof Number) {
            if (!(o instanceof Long || o instanceof Integer || o instanceof Double)) {
                String s = o.toString();
                try {
                    if (s.indexOf('.') != -1) return Double.parseDouble(s);
                    long l = Long.parseLong(s);
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                    return l;
                } catch (NumberFormatException ex) {
                    return s;
                }
            }
        } else if (o != null && o.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            for (int i = 0, length = Array.getLength(o); i < length; i++)
                list.add(Array.get(o, i));
            return mapValues(list);
        }
        return o;
    }

    private Map<String, Object> mapValues(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet())
            result.put(keyMapper.apply(entry.getKey().toString()),
                    mapValue(entry.getValue()));
        return result;
    }

    private List<?> mapValues(Iterable iterable) {
        List<Object> result = new ArrayList<>();
        for (Object o : iterable)
            if (o instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) o;
                MapConfig map = new MapConfig();
                map.set(entry.getKey().toString(), entry.getValue());
                result.add(map);
            } else result.add(mapValue(o));
        return result;
    }

    public MapConfig() {
        this(true);
    }

    public MapConfig(boolean caseInsensitive) {
        parent = new HashMap<>();
        keyMapper = caseInsensitive ? TO_LOWER : Function.identity();
    }

    @Override
    public void set(String path, Object value) {
        MapConfig target = this;

        for (int index; (index = path.indexOf('.')) != -1;) {
            String key = target.keyMapper.apply(path.substring(0, index));
            Object val = target.parent.get(key);
            if (val instanceof MapConfig) target = (MapConfig) val;
            else target.parent.put(key, target = new MapConfig());
            path = path.substring(index + 1);
        }
        path = target.keyMapper.apply(path);
        if (value == null) target.parent.remove(path);
        else target.parent.put(path, mapValue(value));
    }

    @Override
    public void setAll(AbstractConfig config) {
        if (config instanceof MapConfig)
            parent.putAll(((MapConfig) config).parent);
        else parent.putAll(mapValues(config.toRawMap()));
    }

    @Override
    public void setAll(Map<String, Object> map) {
        parent.putAll(mapValues(map));
    }

    @Override
    public Object get(String path) {
        MapConfig target = this;

        for (int index; (index = path.indexOf('.')) != -1;) {
            String key = path.substring(0, index);
            Object val = target.parent.get(key);
            if (val instanceof MapConfig) target = (MapConfig) val;
            else return null;
            path = path.substring(index + 1);
        }
        return target.parent.get(path);
    }

    @Override
    public Set<String> keys() {
        return parent.keySet();
    }

    @Override
    public Map<String, Object> values() {
        return new HashMap<>(parent);
    }

    @Override
    public Set<String> deepKeys() {
        Set<String> result = new HashSet<>();
        deepKeys(parent, result, "");
        return result;
    }
    
    private void deepKeys(Map<?, ?> map, Set<String> result, String path) {
        for (Map.Entry entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            result.add(path + key);
            if (value instanceof Map)
                deepKeys((Map) value, result, path + key + '.');
        }
    }

    @Override
    public Map<String, Object> deepValues() {
        Map<String, Object> map = new HashMap<>();
        deepValues(parent, map, "");
        return map;
    }
    
    private void deepValues(Map<?, ?> map, Map<String, Object> result, String path) {
        for (Map.Entry entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            result.put(path + key, value);
            if (value instanceof Map)
                deepValues((Map) value, result, path + key + '.');
        }
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    @Override
    public Map<String, Object> toRawMap() {
        return convertToRaw(parent);
    }

    private Map<String, Object> convertToRaw(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value instanceof MapConfig) result.put(key, ((MapConfig) value).toRawMap());
            else if (value instanceof List) result.put(key, convertToRaw((List) value));
            else result.put(key, value);
        }
        return result;
    }

    private List<?> convertToRaw(List<?> list) {
        List<Object> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof MapConfig) out.add(((MapConfig) o).toRawMap());
            else if (o instanceof List) out.add(convertToRaw((List) o));
            else out.add(o);
        }
        return out;
    }

    public <T> T serialize(ConfigSerializer<T> serializer) {
        return serializer.serialize(toRawMap());
    }

    public <T> void deserialize(ConfigDeserializer<T> deserializer, T val) {
        parent.putAll(mapValues(deserializer.deserialize(val)));
    }

    public String saveToString() {
        return "";
    }

    public MapConfig loadFromString(String dump) {
        parent.clear();
        return this;
    }

    @Override
    public MapConfig loadFromFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line).append('\n');
        loadFromString(sb.toString());
        stream.close();
        return this;
    }

    @Override
    public void save(File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream));
        bw.write(saveToString() + '\n');
        stream.close();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeBoolean(keyMapper == TO_LOWER); // Case insensitive
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        keyMapper = stream.readBoolean() ? TO_LOWER : Function.identity();
    }

    private static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean;
    }

    public boolean isSection(String path) {
        return get(path) instanceof MapConfig;
    }

    public interface ConfigSerializer<T> {
        T serialize(Map<String, Object> config);
    }

    public interface ConfigDeserializer<T> {
        Map deserialize(T serialized);
    }

    public static void main(String[] args) {
        MapConfig gson = new GsonConfig(),
                  yaml = new YamlConfig(),
                  xml  = new XmlConfig();

        gson.set("key", "value");
        gson.set("path.to.another.key", "1234");
        gson.set("path.to.array",
                new double[][] { { 3.5, 1, 6.8 }, { 1, 17.35 } });
        gson.setIndent(4);

        String saved = gson.saveToString();

        System.out.println(saved);

        gson = new GsonConfig().loadFromString(saved);

        yaml.set("very.long.path.to.a.number", 1337);
        yaml.set("feb.key", false);
        yaml.setAll(gson);
        yaml.setIndent(4);

        saved = yaml.saveToString();

        System.out.println(saved);

        yaml = new YamlConfig().loadFromString(saved);

        xml.set("some.very.random.long", ThreadLocalRandom.current().nextLong());
        xml.set("some.sort.of.list", Arrays.asList(5, "text", true, new int[2]));
        xml.setAll(yaml);
        xml.setIndent(4);

        saved = xml.saveToString();

        System.out.println(saved);

        xml = new XmlConfig().loadFromString(saved);

        System.out.println(xml);
    }
}
