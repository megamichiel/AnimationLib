package me.megamichiel.animationlib.util.db;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.ConfigSection;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SameParameterValue")
public class DataBase {

    public static final int
            DISTINCT = 0x1,
            HIGH_PRIORITY = 0x2,
            STRAIGHT_JOIN = 0x4,
            SQL_SMALL_RESULT = 0x8, SQL_BIG_RESULT = 0x10,
            SQL_BUFFER_RESULT = 0x20,
            SQL_CACHE = 0x40, SQL_NO_CACHE = 0x80,
            SQL_CALC_FOUND_ROWS = 0x100;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("[AnimationLib] Cannot find MySQL driver!");
        }
    }

    private static final Map<String, DataBase> databases = new ConcurrentHashMap<>();
    private static final Map<String, DataBase> byId = new ConcurrentHashMap<>();

    public static DataBase getDataBase(String url) {
        return databases.computeIfAbsent(url, DataBase::new);
    }

    public static DataBase getById(String id) {
        return byId.get(id);
    }

    public static void load(Nagger nagger, ConfigSection config) {
        byId.clear();
        if (config != null) config.forEach((key, value) -> {
            if (!(value instanceof ConfigSection)) return;
            ConfigSection section = (ConfigSection) value;
            String   url = section.getString("url"),
                    user = section.getString("user"),
                    pass = section.getString("password");
            if (url == null) nagger.nag("No url specified in database " + key + "!");
            else if (user == null) nagger.nag("No user specified in database " + key + "!");
            else if (pass == null) nagger.nag("No password specified in database " + key + "!");
            byId.put(section.getOriginalKey(key), DataBase.getDataBase(url).as(user, pass));
        });
    }

    private final String url;
    private String user, pass;

    private Connection connection;

    private DataBase(String url) {
        this.url = url;
    }

    public DataBase as(String user, String pass) {
        this.user = user;
        this.pass = pass;
        return this;
    }

    public boolean isSetup() {
        return user != null && pass != null;
    }

    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    public Connection getConnection() throws SQLException {
        return isConnected() ? connection :
                (connection = DriverManager.getConnection(url, user, pass));
    }

    public Table table(String name) {
        return new Table(name);
    }

    public String url() {
        return url;
    }

    public class Table {

        private final String name;

        private Table(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public SelectStatement select(String... items) {
            return new SelectStatement(this, items);
        }
    }

    public interface Executable {
        Closeable execute() throws SQLException;
        String rawValue();
    }

    public class SelectStatement implements Executable {

        private final Table table;
        private final String[] items;

        private String where;
        private int options, maxStatementTime = -1;

        private SelectStatement(Table table, String[] items) {
            this.table = table;
            this.items = items;
        }

        private boolean has(int option) {
            return (options & option) == option;
        }

        @Override
        public String rawValue() {
            StringBuilder sb = new StringBuilder("SELECT ");

            if (has(DISTINCT)) sb.append("DISTINCT ");
            if (has(HIGH_PRIORITY)) sb.append("HIGH_PRIORITY ");
            if (maxStatementTime != -1) sb.append("MAX_STATEMENT_TIME = ").append(maxStatementTime).append(' ');
            if (has(STRAIGHT_JOIN)) sb.append("STRAIGHT_JOIN");
            if (has(SQL_SMALL_RESULT)) sb.append("SQL_SMALL_RESULT ");
            else if (has(SQL_BIG_RESULT)) sb.append("SQL_BIG_RESULT ");
            if (has(SQL_BUFFER_RESULT)) sb.append("SQL_BUFFER_RESULT ");
            if (has(SQL_CACHE)) sb.append("SQL_CACHE ");
            else if (has(SQL_NO_CACHE)) sb.append("SQL_NO_CACHE ");
            if (has(SQL_CALC_FOUND_ROWS)) sb.append("SQL_CALC_FOUND_ROWS ");

            sb.append('`').append(items[0]);
            for (int i = 1; i < items.length; i++)
                sb.append("`,`").append(items[i]);
            sb.append("` FROM `").append(table.name).append("` ");

            if (where != null) sb.append("WHERE ").append(where);

            return sb.toString();
        }

        @Override
        public ResultHolder execute() throws SQLException {
            Statement sm = getConnection().createStatement();
            ResultSet set = sm.executeQuery(rawValue());
            return new ResultHolder(set, set, sm);
        }

        public SelectStatement options(int options) {
            this.options = options;
            return this;
        }

        public SelectStatement maxTime(int time) {
            maxStatementTime = time;
            return this;
        }

        public SelectStatement where(String expr) {
            where = expr;
            return this;
        }
    }

    public static class MultiCloseable implements Closeable {

        private final AutoCloseable[] closeables;

        public MultiCloseable(AutoCloseable... closeables) {
            this.closeables = closeables;
        }

        @Override
        public void close() throws IOException {
            List<Exception> thrown = new ArrayList<>();
            for (AutoCloseable closeable : closeables) {
                try { // Make sure every closeable has had a close attempt
                    closeable.close();
                } catch (Exception ex) {
                    thrown.add(ex);
                }
            }
            if (thrown.size() == 1) {
                throw thrown.get(0) instanceof IOException ? (IOException) thrown.get(0) : new IOException(thrown.get(0));
            }
            if (!thrown.isEmpty()) {
                throw new MultiCloseException(thrown);
            }
        }
    }

    public static class MultiCloseException extends IOException {

        private static final long serialVersionUID = -6759415315426709197L;

        private final Exception[] sources;

        public MultiCloseException(List<Exception> sources) {
            super(sources.size() + " exceptions were thrown");
            this.sources = sources.toArray(new Exception[sources.size()]);
        }

        public Exception[] getSources() {
            return sources;
        }
    }

    public class ResultHolder extends MultiCloseable {

        private final ResultSet res;

        public ResultHolder(ResultSet res, AutoCloseable... closeables) {
            super(closeables);
            this.res = res;
        }

        public ResultSet resultSet() {
            return res;
        }
    }
}
