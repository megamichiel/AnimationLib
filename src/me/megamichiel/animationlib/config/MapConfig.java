package me.megamichiel.animationlib.config;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MapConfig extends ConfigSection implements Serializable {

    private static final long serialVersionUID = -2756209354518932499L;

    private final Map<String, String> mappedKeys = new HashMap<>();
    private final Map<String, Object> parent;
    private boolean caseInsensitive;

    public int size() {
        return parent.size();
    }

    @Override
    public String getOriginalKey(String key) {
        return mappedKeys.getOrDefault(key, key);
    }

    @Override
    public void restoreKeys(boolean deep) {
        caseInsensitive = false;
        super.restoreKeys(deep);
    }

    private Object mapValue(Object o) {
        if (o instanceof Map) {
            return new MapConfig((Map) o, caseInsensitive);
        } else if (o instanceof Iterable) {
            return StreamSupport.stream(((Iterable<?>) o).spliterator(), false).map(this::mapValue).collect(Collectors.toList());
        } else if (o instanceof String) {
            String s = ((String) o).toLowerCase(Locale.ENGLISH);
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
            } else if (o instanceof Double) {
                double d = (double) o;
                long l = (long) d;
                if (l == d) {
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                    return l;
                }
            }
        } else if (o instanceof Map.Entry) {
            Map.Entry entry = (Map.Entry) o;
            MapConfig map = new MapConfig(caseInsensitive);
            map.set(entry.getKey().toString(), map.mapValue(entry.getValue()));
            return map;
        } else if (o != null && o.getClass().isArray()) {
            int i = 0, length = Array.getLength(o);
            List<Object> list = new ArrayList<>();
            while (i < length) {
                list.add(mapValue(Array.get(o, i++)));
            }
            return list;
        }
        return o;
    }

    private Map<String, Object> mapValues(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            String mapped = caseInsensitive ? key.toLowerCase(Locale.ENGLISH) : key;
            mappedKeys.put(mapped, key);
            result.put(mapped, mapValue(entry.getValue()));
        }
        return result;
    }

    public MapConfig() {
        this(true);
    }

    public MapConfig(boolean caseInsensitive) {
        parent = new LinkedHashMap<>();
        this.caseInsensitive = caseInsensitive;
    }

    public MapConfig(Map<?, ?> map) {
        this(map, true);
    }

    public MapConfig(Map<?, ?> map, boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        parent = mapValues(map);
    }

    @Override
    public void set(String path, Object value) {
        MapConfig target = this;

        for (int index; (index = path.indexOf('.')) != -1;) {
            String key = path.substring(0, index),
                   mapped = target.caseInsensitive ? key.toLowerCase(Locale.ENGLISH) : key;
            Object val = target.parent.get(mapped);
            if (val instanceof MapConfig) target = (MapConfig) val;
            else {
                target.parent.put(mapped, target = new MapConfig(caseInsensitive));
                target.mappedKeys.put(mapped, key);
            }
            path = path.substring(index + 1);
        }

        String key = target.caseInsensitive ? path.toLowerCase(Locale.ENGLISH) : path;
        if (value != null) {
            target.parent.put(key, mapValue(value));
            target.mappedKeys.put(key, path);
        } else if (target.parent.remove(key) != null) {
            target.mappedKeys.remove(key);
        }
    }

    @Override
    public void setAll(AbstractConfig config) {
        parent.putAll(mapValues(config.toRawMap()));
    }

    @Override
    public void setAll(ConfigSection config) {
        parent.putAll(mapValues(config.toRawMap()));
    }

    @Override
    public void setAll(Map<?, ?> map) {
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
        return new HashSet<>(parent.keySet());
    }

    @Override
    public Map<String, Object> values() {
        return new LinkedHashMap<>(parent);
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
        Map<String, Object> map = new LinkedHashMap<>();
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
    public Map<String, Object> toRawMap() {
        return convertToRaw(parent);
    }

    private Map<String, Object> convertToRaw(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
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
        Map map = deserializer.deserialize(val);
        if (map != null) parent.putAll(mapValues(map));
    }

    public String saveToString() {
        return "";
    }

    public void loadFromString(String dump) {
        parent.clear();
    }

    @Override
    public void loadFromFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        loadFromString(sb.toString());
        stream.close();
    }

    @Override
    public void save(File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream));
        bw.write(saveToString() + '\n');
        bw.close();
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MapConfig && ((MapConfig) obj).parent.equals(parent);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    public interface ConfigSerializer<T> {
        T serialize(Map<String, Object> config);
    }

    public interface ConfigDeserializer<T> {
        Map deserialize(T serialized);
    }
}
