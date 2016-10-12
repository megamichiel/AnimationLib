package me.megamichiel.animationlib.command.arg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A built-in CustomArgument. Much like minecraft's target selector, with nearly everything implemented.
 */
public class EntitySelector implements CustomArgument {
    
    private static final Pattern
            SELECTOR_PATTERN = Pattern.compile("^@([pare])(?:\\[([\\w=,!-]*)\\])?$"),
            VALUE_PATTERN = Pattern.compile("\\G([-!]?[\\w-]*)(?:$|,)"),
            KEY_VALUE_PATTERN = Pattern.compile("\\G(\\w+)=([-!]?[\\w-]*)(?:$|,)");
    private static final Map<?, ?> entityByName;
    
    static {
        Map<?, ?> map = Collections.emptyMap();
        try {
            String pkg = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> entityTypes = Class.forName(pkg + ".EntityTypes");
            for (Field field : entityTypes.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Map.class) {
                    field.setAccessible(true);
                    Map<?, ?> m = (Map<?, ?>) field.get(null);
                    if (!m.isEmpty()) {
                        Entry<?, ?> sample = m.entrySet().iterator().next();
                        if (sample.getKey() instanceof String && sample.getValue() instanceof Integer) {
                            map = m;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        entityByName = map;
    }
    
    private final List<Entity> entities;
    
    @SuppressWarnings("deprecation")
    public EntitySelector(CommandSender sender, String value) {
        Player player = Bukkit.getPlayerExact(value);
        if (player != null) {
            entities = Collections.<Entity>singletonList(player);
            return;
        }
        List<Entity> entities = new ArrayList<>();
        Location from = sender instanceof Player ? ((Player) sender).getLocation() : null;
        Matcher matcher = SELECTOR_PATTERN.matcher(value);
        if (matcher.matches()) {
            char id = matcher.group(1).charAt(0);
            EntitySelection sel = parseArguments(from, id, matcher.group(2));
            switch (id) {
            case 'a': case 'p':
                entities.addAll(Bukkit.getOnlinePlayers());
                break;
            case 'r':
                if (sel.entityType == null) {
                    entities.addAll(Bukkit.getOnlinePlayers());
                    break;
                }
                // Fall-through, add entities by that type
            case 'e':
                for (World world : Bukkit.getWorlds())
                    for (Entity entity : world.getEntities())
                        if (sel.entityType == null || (entity.getType() == sel.entityType) != sel.negateEntityType)
                            entities.add(entity);
                break;
            }
            this.entities = sel.filter(entities);
        } else throw new IllegalArgumentException("No entities found by that name");
    }
    
    private EntitySelection parseArguments(Location from, char id, String str)
    {
        Map<String, String> map = new HashMap<>();
        if (str == null) return new EntitySelection(from, id, map);
        
        int unnamed = 0;
        int index = -1;
        Matcher matcher = VALUE_PATTERN.matcher(str);
        while (matcher.find()) {
            char key = 0;
            switch (unnamed++) {
            case 0:
                key = 'x';
                break;
            case 1:
                key = 'y';
                break;
            case 2:
                key = 'z';
                break;
            case 3:
                key = 'r';
                break;
            }
            if (key != 0 && matcher.group(1).length() > 0)
                map.put(Character.toString(key), matcher.group(1));
            index = matcher.end();
        }
        if (index < str.length()) {
            matcher = KEY_VALUE_PATTERN.matcher(index == -1 ? str : str.substring(index));
            while (matcher.find())
                map.put(matcher.group(1), matcher.group(2));
        }
        
        return new EntitySelection(from, id, map);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public String toString() {
        return this.entities.toString();
    }
    
    private static class EntitySelection {
        
        private final Location from;
        private final char id;
        private final Vector volume;
        private final int minYaw, maxYaw, minPitch, maxPitch;
        private final int radius, minRadius, count, level, minLevel;
        private final int gameMode;
        private final EntityType entityType;
        private final boolean negateEntityType, negateName;
        private final String name;

        private final Map<String, Integer> minScores = new HashMap<>(),
                maxScores = new HashMap<>();
        private final String team;
        private final boolean negateTeam;
        
        @SuppressWarnings("deprecation")
        EntitySelection(Location from, char id, Map<String, String> map) {
            this.id = id;
            double[] position;
            if (from != null)
                position = new double[] { from.getX(), from.getY(), from.getZ() };
            else position = new double[3];
            char[] types = "xyz".toCharArray();
            for (int i = 0; i < 3; i++) {
                String str = map.get(Character.toChars(types[i]));
                if (str != null) {
                    try {
                        position[i] = Integer.parseInt(str) + .5;
                    } catch (NumberFormatException ex) {
                        // Invalid x/y/z
                    }
                }
            }
            if (from != null)
                this.from = new Location(from.getWorld(), position[0], position[1], position[2]);
            else this.from = null;
            volume = new Vector(parse(map, "dx", 0), parse(map, "dy", 0), parse(map, "dz", 0));
            minYaw = angle(parse(map, "rym", 0));
            maxYaw = angle(parse(map, "ry", 359));
            minPitch = angle(parse(map, "rxm", 0));
            maxPitch = angle(parse(map, "rx", 359));
            radius = parse(map, "r", 0);
            minRadius = parse(map, "rm", 0);
            count = parse(map, "c", 0);
            level = parse(map, "l", 0);
            minLevel = parse(map, "lm", 0);
            String mode = map.get("m");
            int gameMode = -1;
            if (mode != null) switch (mode) {
                case "3": case "sp": case "spectator":
                    gameMode++;
                case "2": case "a": case "adventure":
                    gameMode++;
                case "1": case "c": case "creative":
                    gameMode++;
                case "0": case "s": case "survival":
                    gameMode++;
            }
            this.gameMode = gameMode;
            EntityType entityType = null;
            boolean negateEntityType = false;
            switch (id) {
                case 'a': case 'p':
                    entityType = EntityType.PLAYER;
                    break;
                case 'r':
                    entityType = EntityType.PLAYER;
                    // Fall-through, if no type is specified, it defaults to player
                case 'e':
                    if (map.containsKey("type")) {
                        String type = map.get("type");
                        if (type.startsWith("!")) {
                            negateEntityType = true;
                            type = type.substring(1);
                        }
                        Object eid = entityByName.get(type);
                        if (eid instanceof Integer)
                            entityType = EntityType.fromId((Integer) eid);
                        else if ("Player".equals(type))
                            entityType = EntityType.PLAYER;
                        else if ("LightningBolt".equals(type))
                            entityType = EntityType.LIGHTNING;
                    }
                    break;
            }
            this.entityType = entityType;
            this.negateEntityType = negateEntityType;
            String name = map.get("name");
            if (name != null && name.startsWith("!")) {
                negateName = true;
                name = name.substring(1);
            } else negateName = false;
            this.name = name;

            for (Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().startsWith("score_")) {
                    String score = entry.getKey().substring(6);
                    boolean min = score.length() > 4 && score.endsWith("_min");
                    if (min) score = score.substring(0, score.length() - 4);
                    int value;
                    try {
                        value = Integer.parseInt(entry.getValue());
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    (min ? minScores : maxScores).put(score, value);
                }
            }
            name = map.get("team");
            if (name != null) {
                if (name.isEmpty()) {
                    negateTeam = true;
                    team = null;
                } else {
                    negateTeam = name.charAt(0) == '!';
                    team = negateTeam ? name.substring(1) : name;
                }
            } else {
                team = null;
                negateTeam = false;
            }
        }
        
        private boolean matches(Entity entity) {
            if (name != null && name.equals(entity.getName()) == negateName)
                return false;
            if (id == 'p' && entity.isDead()) // No dead players here ;o
                return false;
            if (level > 0) {
                if (!(entity instanceof Player)) return false;
                Player player = (Player) entity;
                if (player.getLevel() > level || player.getLevel() < minLevel)
                    return false;
            }
            if (gameMode > -1) {
                if (!(entity instanceof Player)) return false;
                switch (((Player) entity).getGameMode()) {
                    case SURVIVAL:
                        if (gameMode != 0) return false;
                        break;
                    case CREATIVE:
                        if (gameMode != 1) return false;
                        break;
                    case ADVENTURE:
                        if (gameMode != 2) return false;
                        break;
                    case SPECTATOR:
                        if (gameMode != 3) return false;
                        break;
                }
            }
            if (from != null) {
                Location loc = entity.getLocation();
                if (radius > 0) {
                    if (entity.getWorld() != from.getWorld()) return false;
                    double dist = loc.distanceSquared(from);
                    if (dist > (radius * radius) || dist < (minRadius * minRadius)) {
                        return false;
                    }
                }
                if (volume.getX() > 0 && Math.abs(loc.getBlockX() - from.getBlockX()) > volume.getBlockX())
                    return false;
                if (volume.getY() > 0 && Math.abs(loc.getBlockY() - from.getBlockY()) > volume.getBlockY())
                    return false;
                if (volume.getZ() > 0 && Math.abs(loc.getBlockZ() - from.getBlockZ()) > volume.getBlockZ())
                    return false;
                int yaw = angle(loc.getYaw()), pitch = angle(loc.getPitch());
                if (yaw < minYaw || yaw > maxYaw) return false;
                if (pitch < minPitch || pitch > maxPitch) return false;
            }
            String name = entity instanceof Player ? entity.getName() : entity.getUniqueId().toString();
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Entry<String, Integer> entry : maxScores.entrySet()) {
                Objective obj = board.getObjective(entry.getKey());
                if (obj == null) continue;
                if (obj.getScore(name).getScore() > entry.getValue())
                    return false;
            }
            for (Entry<String, Integer> entry : minScores.entrySet()) {
                Objective obj = board.getObjective(entry.getKey());
                if (obj == null) continue;
                if (obj.getScore(name).getScore() < entry.getValue())
                    return false;
            }
            Team team = board.getTeam(name);
            if (this.team != null
                    && (team != null && this.team.equals(team.getName())) == negateTeam)
                return false;
            else if (this.team == null && negateTeam && team != null)
                return false;
            return true;
        }

        private int angle(float angle) {
            int i = (int) angle;
            if (i > angle) i--;
            i %= 360;
            if (i >= 180) i -= 360;
            else if (i < -180) i += 360;
            return i;
        }
        
        List<Entity> filter(List<Entity> list) {
            for (Iterator<Entity> it = list.iterator(); it.hasNext();)
                if (!matches(it.next()))
                    it.remove();
            if ((id == 'p' || id == 'a' || id == 'e') && from != null) {
                Collections.sort(list, new Comparator<Entity>() {
                    @Override
                    public int compare(Entity a, Entity b) {
                        if (radius > 0) {
                            double da = a.getLocation().distanceSquared(from),
                                    db = b.getLocation().distanceSquared(from);
                            if (da < db)
                                return -1;
                            else if (db > da)
                                return 1;
                            else {
                                int ta = a.getTicksLived(),
                                        tb = b.getTicksLived();
                                return ta < tb ? -1 : tb > ta ? 1 : 0;
                            }
                        }
                        return 0;
                    }
                });
            }
            if (id == 'r') Collections.shuffle(list);
            if (count > 0 && count < list.size())
                list = list.subList(0, count);
            else if (count < 0 && -count < list.size()) {
                int to = list.size();
                list = list.subList(to + count, to);
            }
            return list;
        }

        private int parse(Map<String, String> map, String str, int def) {
            try {
                return Integer.parseInt(map.get(str));
            } catch (Exception ex) {
                return def;
            }
        }
    }
}
