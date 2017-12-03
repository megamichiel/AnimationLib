package me.megamichiel.animationlib.util.db;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.placeholder.StringBundle;

import javax.script.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SQLHandler implements Runnable {

    private static SQLHandler instance;

    public static SQLHandler getInstance() {
        return instance;
    }

    private final AnimLib lib;
    private final ScriptEngine engine;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private long refreshDelay;

    public SQLHandler(AnimLib lib) {
        if (instance != null) throw new IllegalStateException("Already initialized!");
        instance = this;
        this.lib = lib;
        engine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    public long getRefreshDelay() {
        return refreshDelay;
    }

    public void load(ConfigSection section) {
        entries.clear();
        if (section != null) {
            refreshDelay = Math.max(section.getInt("refresh-delay", 1), 5);
            section.forEach((key, value) -> {
                if (value instanceof ConfigSection) {
                    try {
                        entries.put(section.getOriginalKey(key), new Entry((ConfigSection) value));
                    } catch (IllegalArgumentException e) {
                        lib.nag("Failed to load sql query " + key + "!");
                        lib.nag(e);
                    }
                }
            });
        } else refreshDelay = 0;
    }

    @Override
    public void run() {
        for (Entry entry : entries.values()) {
            if (entry.autoRefresh) entry.refresh();
            else if (entry.shouldRefresh) {
                entry.refresh();
                entry.shouldRefresh = false;
            }
        }
    }

    public Entry getEntry(String name) {
        return entries.get(name);
    }

    public void awaitRefresh(Object player, List<Entry> entries,
                             Runnable onComplete) {
        lib.post(() -> {
            refresh(player, entries);
            lib.post(onComplete, false);
        }, true);
    }

    public void refresh(Object player, List<Entry> entries) {
        if (player != null) {
            for (Entry entry : entries) {
                entry.refresh(player);
            }
        } else entries.forEach(Entry::refresh);
    }

    public String get(String key, Object player) {
        Entry entry = entries.get(key);
        if (entry == null) return "<unknown_query>";
        return entry.get(player);
    }

    public String getAndRefresh(String key, Object player) {
        Entry entry = entries.get(key);
        if (entry == null) return "<unknown_query>";
        String s = entry.get(player);
        entry.requestRefresh();
        return s;
    }

    public void playerJoin(Object player) {
        entries.values().forEach(e -> {
            if (e.requestOnJoin) e.get(player);
        });
    }

    public void playerQuit(Object player) {
        entries.values().forEach(e -> e.sessions.remove(player));
    }

    public class Entry {

        private final DataBase db;
        private final boolean requestOnJoin, autoRefresh;
        private final StringBundle query, def;
        private final int lifespan;
        private boolean shouldRefresh;

        private final CompiledScript script;
        private final Map<Object, Session> sessions = new ConcurrentHashMap<>();

        Entry(ConfigSection section) throws IllegalArgumentException {
            String id = section.getString("database"),
                   query = section.getString("query"),
                   def = section.getString("default", "<loading>");
            if (id == null) throw new IllegalArgumentException("No URL specified!");
            db = DataBase.getById(id);
            if (db == null)
                throw new IllegalArgumentException("Unknown database: " + id);
            if (query == null) throw new IllegalArgumentException("No query specified!");
            this.query = StringBundle.parse(lib, query);
            this.def = StringBundle.parse(lib, def);
            lifespan = section.getInt("lifespan", 0);
            requestOnJoin = section.getBoolean("request-on-join");
            autoRefresh = section.getBoolean("auto-refresh");

            List<String> list = section.getStringList("script");
            String script;
            if (!list.isEmpty()) script = String.join("", list);
            else if ((script = section.getString("script")) == null)
                throw new IllegalArgumentException("Unexpected script value");
            try {
                this.script = ((Compilable) engine).compile(script);
            } catch (ScriptException e) {
                throw new IllegalArgumentException("Failed to parse script", e);
            }
        }

        public void refresh() {
            try {
                Connection con = db.getConnection();
                try (Statement sm = con.createStatement()) {
                    for (Map.Entry<Object, Session> entry : sessions.entrySet()) {
                        Object player = entry.getKey();
                        try (ResultSet res = sm.executeQuery(query.toString(player))) {
                            Bindings b = engine.createBindings();
                            b.put("sql", res);
                            entry.getValue().update(String.valueOf(script.eval(b)), lifespan);
                        }
                    }
                }
            } catch (SQLException ex) {
                lib.nag("Failed to connect to database " + db.url() + "!");
                lib.nag(ex);
            } catch (ScriptException ex) {
                lib.nag("Error while executing script!");
                lib.nag(ex);
            }
        }

        public boolean refresh(Object player) {
            try {
                Connection con = db.getConnection();
                try (Statement sm = con.createStatement()) {
                    try (ResultSet res = sm.executeQuery(query.toString(player))) {
                        Bindings b = engine.createBindings();
                        b.put("sql", res);
                        String value = String.valueOf(script.eval(b));
                        sessions.compute(player, (p, session) -> session == null
                                ? new Session(value, lifespan) : session.update(value, lifespan));
                        return true;
                    }
                }
            } catch (SQLException ex) {
                lib.nag("Failed to connect to database " + db.url() + "!");
                lib.nag(ex);
            } catch (ScriptException ex) {
                lib.nag("Error while executing script!");
                lib.nag(ex);
            }
            return false;
        }

        String get(Object player) {
            Session session = sessions.computeIfAbsent(player, p ->
                    new Session(def.toString(p), lifespan));
            if (session.lifespan > 0 && --session.lifespan == 0) {
                sessions.remove(player);
            }
            return session.value;
        }

        void requestRefresh() {
            shouldRefresh = true;
        }
    }

    private static class Session {

        private String value;
        private int lifespan;

        Session(String value, int lifespan) {
            this.value = value;
            this.lifespan = lifespan;
        }

        Session update(String value, int lifespan) {
            this.value = value;
            this.lifespan = lifespan;
            return this;
        }
    }
}
