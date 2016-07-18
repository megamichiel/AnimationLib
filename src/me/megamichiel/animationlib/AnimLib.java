package me.megamichiel.animationlib;

import static org.bukkit.ChatColor.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public interface AnimLib {

    /**
     * Retrieves the current version of a resource on spigotmc.org<br/>
     * <i>Since: 1.1.0</i>
     *
     * @param resource the id of the resource
     * @return the latest version
     * @throws IOException if it can't connect
     */
    static String getVersion(int resource) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(
                "http://www.spigotmc.org/api/general.php").openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.getOutputStream()
                .write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=" + resource)
                        .getBytes("UTF-8"));
        String version = new BufferedReader(new InputStreamReader(
                con.getInputStream())).readLine();
        if (version.length() <= 7) {
            return version;
        }
        throw new IOException("Unexpected response");
    }
}
