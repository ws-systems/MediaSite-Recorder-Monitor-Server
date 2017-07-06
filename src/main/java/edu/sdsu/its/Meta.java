package edu.sdsu.its;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Tom Paulus
 *         Created on 7/5/17.
 */
public class Meta {
    private static final Logger LOGGER = Logger.getLogger(Meta.class);
    public static final MetaInfo buildInfo = Meta.loadBuildInfo();

    public static MetaInfo loadBuildInfo() {
        InputStream inputStream = Meta.class.getClassLoader().getResourceAsStream("build_info.properties");
        Properties properties;
        try {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.error("Problem Reading BuildInfo properties file", e);
            return null;
        }
        return new MetaInfo(
                properties.getProperty("version"),
                properties.getProperty("build"),
                properties.getProperty("time"),
                properties.getProperty("host"),
                properties.getProperty("user")
        );
    }

    public static class MetaInfo {
        String version;
        String build;
        String time;
        String host;
        String user;

        MetaInfo(String version, String build, String time, String host, String user) {
            this.version = version;
            this.build = build;
            this.time = time;
            this.host = host;
            this.user = user;
        }

        public String getVersion() {
            return version;
        }

        public String getBuild() {
            return build;
        }

        public String getTime() {
            return time;
        }
    }
}
