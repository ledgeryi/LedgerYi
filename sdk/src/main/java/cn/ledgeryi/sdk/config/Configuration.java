package cn.ledgeryi.sdk.config;

import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.LedgerYiUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
public class Configuration {

    @Getter
    private static Config config;

    @Getter
    private static String accountyiAddress;

    @Getter
    private static String accountyiPrivateKey;

    @Getter
    private static boolean isEcc = true;

    private static final String CRYPTO_ENGINE = "crypto.engine";
    private static final String ACCOUNTYI_ADDRESS = "accountyi.address";
    private static final String ACCOUNTYI_PRIVATE_KEY = "accountyi.privateKeyStr";

    static {
        Config config = Configuration.getByPath("config.conf");
        if (config.hasPath(CRYPTO_ENGINE)) {
            isEcc = config.getString(CRYPTO_ENGINE).equalsIgnoreCase("ecc");
            log.info("is ecc engine: " + isEcc);
        }

        if (config.hasPath(ACCOUNTYI_PRIVATE_KEY) && config.hasPath(ACCOUNTYI_ADDRESS)) {
            if (StringUtils.isNotEmpty(config.getString(ACCOUNTYI_PRIVATE_KEY)) && StringUtils.isNotEmpty(config.getString(ACCOUNTYI_ADDRESS))) {
                log.warn("account's address and privateKey are not empty, it will use default account");
                accountyiAddress = config.getString(ACCOUNTYI_ADDRESS);
                accountyiPrivateKey = config.getString(ACCOUNTYI_PRIVATE_KEY);
            } else {
                log.warn("account's address and privateKey are empty, it will be create a default account");
                AccountYi accountYi = LedgerYiUtils.createAccountYi();
                accountyiAddress = accountYi.getAddress();
                accountyiPrivateKey = accountYi.getPrivateKeyStr();
                ObjectMapper mapper = new ObjectMapper();
                File file = new File("accountYi.json");
                if (file.exists()) {
                    try {
                        AccountYi defaultAccount = mapper.readValue(file, AccountYi.class);
                        accountyiAddress = defaultAccount.getAddress();
                        accountyiPrivateKey = defaultAccount.getPrivateKeyStr();
                    } catch (IOException e) {
                        log.error("read default account error, error: ", e);
                    }
                }
                try {
                    mapper.writeValue(file, accountYi);
                } catch (IOException e) {
                    log.error("write default account error, error: ", e);
                }
            }
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
