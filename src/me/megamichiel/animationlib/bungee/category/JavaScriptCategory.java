package me.megamichiel.animationlib.bungee.category;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.script.*;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class JavaScriptCategory extends PlaceholderCategory {

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
    private final Map<String, BiFunction<Nagger, ProxiedPlayer, String>> scripts = new ConcurrentHashMap<>();

    public JavaScriptCategory() {
        super("javascript");
    }

    @Override
    public void onEnable(AnimLibPlugin plugin) {
        ConfigManager<YamlConfig> manager = ConfigManager.of(YamlConfig::new)
                .file(new File(plugin.getDataFolder(), "javascript_placeholders.yml"));
        manager.saveDefaultConfig(() -> plugin.getResourceAsStream("javascript_placeholders.yml"));
        YamlConfig config = manager.getConfig();
        config.values().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ConfigSection)
                .forEach(entry -> {
                    String key = entry.getKey();
                    ConfigSection value = (ConfigSection) entry.getValue();
                    String script = value.getString("script"),
                            trueResult = value.getString("true-result"),
                            falseResult = value.getString("false-result");
                    if (script == null)
                        plugin.nag("No script specified in a javascript placeholder!");
                    else {
                        if (trueResult == null) trueResult = plugin.booleanTrue();
                        if (falseResult == null) falseResult = plugin.booleanFalse();
                        scripts.put(key, compile(plugin, script, trueResult, falseResult));
                    }
                });
    }

    @Override
    public RegisteredPlaceholder get(String value) {
        BiFunction<Nagger, ProxiedPlayer, String> script = scripts.get(value.toLowerCase(Locale.US));
        return script != null ? script::apply : (n, p) -> "<unknown_script>";
    }

    private BiFunction<Nagger, ProxiedPlayer, String> compile(AnimLibPlugin plugin, String script,
                                              String trueResult, String falseResult) {
        StringBundle bundle = StringBundle.parse(plugin, script);
        CompiledScript compiled;
        if (bundle.containsPlaceholders()) compiled = new CompiledScript() {
            @Override
            public Object eval(ScriptContext context) throws ScriptException {
                return engine.eval(bundle.invoke(
                        (Nagger) context.getAttribute("nagger"),
                        context.getAttribute("player")
                ));
            }

            @Override
            public ScriptEngine getEngine() {
                return engine;
            }
        };
        else if (engine instanceof Compilable) {
            try {
                compiled = ((Compilable) engine).compile(script);
            } catch (ScriptException ex) {
                plugin.nag("Unable to compile " + script + "!");
                plugin.nag(ex);
                return (a, b) -> "<invalid_script>";
            }
        } else compiled = new CompiledScript() {
            @Override
            public Object eval(ScriptContext context) throws ScriptException {
                return engine.eval(script);
            }

            @Override
            public ScriptEngine getEngine() {
                return engine;
            }
        };
        ScriptContext ctx = new SimpleScriptContext();
        Bindings bindings = new SimpleBindings();
        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        ctx.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        return (nagger, player) -> {
            bindings.put("nagger", nagger);
            bindings.put("player", player);
            try {
                Object o = compiled.eval(ctx);
                if (o instanceof Boolean) return (Boolean) o ? trueResult : falseResult;
                return o.toString();
            } catch (ScriptException e) {
                nagger.nag(e);
                return "<execution_failed>";
            }
        };
    }
}
