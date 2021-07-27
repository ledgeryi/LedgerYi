package cn.ledgeryi.sdk.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
public class Configuration {

    @Getter
    private static Config config;

    @Getter
    private static boolean isEcc = true;

    private static final String CRYPTO_ENGINE = "crypto.engine";

    static {
        config = Configuration.getByPath("config.conf");
        if (config.hasPath(CRYPTO_ENGINE)) {
            isEcc = config.getString(CRYPTO_ENGINE).equalsIgnoreCase("ecc");
            log.info("is ecc engine: " + isEcc);
        }
    }

    /**
     * Get configuration by a given path.
     */
    public static Config getByPath(final String configurationPath) {
        if (isBlank(configurationPath)) {
            throw new IllegalArgumentException("Configuration path is required!");
        }

        if (config == null) {
            File configFile = new File(System.getProperty("user.dir") + '/' + configurationPath);
            if (configFile.exists()) {
                try {
                    config = ConfigFactory.parseReader(new InputStreamReader(new FileInputStream(configurationPath)));
                    log.debug("use user defined config file in current dir");
                } catch (FileNotFoundException e) {
                    log.error("load user defined config file exception: " + e.getMessage());
                }
            } else {
                config = ConfigFactory.load(configurationPath);
                log.debug("user defined config file doesn't exists, use default config file in jar");
            }
        }
        return config;
    }

    public static String getLedgerYiNet() {
        String ledgerYiNode;
        if (config.hasPath("ledgernode.ip.list") && config.getStringList("ledgernode.ip.list").size() != 0) {
            ledgerYiNode = config.getStringList("ledgernode.ip.list").get(0);
        } else {
            throw new RuntimeException("No connection information is configured!");
        }
        return ledgerYiNode;
    }
}
