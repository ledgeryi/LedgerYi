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
  private static boolean isEckey = true;

  static {
    Config config = Configuration.getByPath("config.conf");
    if (config.hasPath("crypto.engine")) {
      isEckey = config.getString("crypto.engine").equalsIgnoreCase("eckey");
      log.debug("Sha256Sm3Hash getConfig isEckey: " + isEckey);
    }
  }

  /**
   * Get configuration by a given path.
   */
  private static Config getByPath(final String configurationPath) {
    if (isBlank(configurationPath)) {
      throw new IllegalArgumentException("Configuration path is required!");
    }

    if (config == null) {
      File configFile = new File(System.getProperty("user.dir")+'/'+configurationPath);
      if(configFile.exists()){
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
}
