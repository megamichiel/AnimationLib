package me.megamichiel.animationlib.config.type;

import me.megamichiel.animationlib.config.MapConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Map;

public class YamlConfig extends MapConfig {

    private static final long serialVersionUID = -3738784362967660224L;

    private final transient Representer     representer = new Representer();
    private final transient DumperOptions   options     = new DumperOptions();
    private final transient Yaml            yaml        = new Yaml(representer, options);

    {
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }

    @Override
    public void setIndent(int indent) {
        super.setIndent(indent);
        options.setIndent(indent);
    }

    @Override
    public String saveToString() {
        String result = serialize(yaml::dump);
        return result.equals("{}\n") ? "" : result;
    }

    @Override
    public void loadFromString(String dump) {
        super.loadFromString(dump);
        deserialize(s -> yaml.loadAs(s, Map.class), dump);
    }
}
