package me.megamichiel.animationlib;

import me.megamichiel.animationlib.util.LoggerNagger;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public interface AnimLib<E> extends LoggerNagger, PipelineContext {

    <T extends E> Pipeline<T> newPipeline(Class<T> type);

    /**
     * Retrieves the current version of a resource on spigotmc.org<br/>
     * <i>Since: 1.1.0</i>
     *
     * @param resource the id of the resource
     * @return the latest version
     * @throws IOException if it can't connect ofc
     *
     * @deprecated Seems this "feature" has disappeared. Gonna be moving
     */
    @Deprecated
    static String getVersion(int resource) throws IOException {
        switch (resource) {
            case 22295:
                return getVersion("AnimationLib");
            case 4690:
                return getVersion("AnimatedMenu");
        }

        HttpURLConnection con = (HttpURLConnection) new URL(
                "http://www.spigotmc.org/api/general.php").openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.getOutputStream()
                .write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=" + resource)
                        .getBytes("UTF-8"));
        String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
        if (version == null) {
            throw new IOException("No response from server. Nag the author of this plugin for an update if there isn't one yet.");
        }
        if (version.length() <= 7) {
            return version;
        }
        throw new IOException("Unexpected response");
    }

    static String getVersion(String resource) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(
                "https://raw.githubusercontent.com/megamichiel/AnimationLib/master/versions.txt"
        ).openConnection();

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        for (String line; (line = reader.readLine()) != null; ) {
            int index = line.indexOf('=');
            if (index > 0 && line.regionMatches(0, resource, 0, index)) {
                return line.substring(index + 1);
            }
        }
        return null;
    }
}
