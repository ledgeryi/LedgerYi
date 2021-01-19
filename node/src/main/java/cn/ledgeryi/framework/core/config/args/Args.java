package cn.ledgeryi.framework.core.config.args;

import cn.ledgeryi.chainbase.core.config.Parameter.NetConstants;
import cn.ledgeryi.chainbase.core.config.Parameter.NodeConstant;
import cn.ledgeryi.chainbase.common.storage.rocksdb.RocksDbSettings;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.config.args.Account;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.common.core.Constant;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.Configuration;
import cn.ledgeryi.framework.core.db.backup.DbBackupConfig;
import cn.ledgeryi.chainbase.core.config.args.Master;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static cn.ledgeryi.consenus.base.Constant.BLOCK_PRODUCE_TIMEOUT_PERCENT;
import static java.lang.Math.max;

@Slf4j(topic = "app")
@NoArgsConstructor
@Component
public class Args {

  private static final Args INSTANCE = new Args();

  @Parameter(names = {"-c", "--config"}, description = "Config File")
  private String shellConfFileName = "";

  //@Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = "output-directory";

  @Getter
  //@Parameter(names = {"--log-config"})
  private String logbackPath = "";

  @Getter
  //@Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
  private boolean help = false;

  @Getter
  @Setter
  @Parameter(names = {"-m", "--master"})
  private boolean master = false;

  @Getter
  @Setter
  //@Parameter(names = {"--debug"})
  private boolean debug = false;

  @Getter
  @Setter
  //@Parameter(names = {"--max-connect-number"})
  private int maxHttpConnectNumber = 50;

  @Getter
  @Parameter(description = "--seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = "";

  @Parameter(names = {"--master-address"}, description = "master-address")
  private String masterAddress = "";

  //@Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  private String storageDbDirectory = "";

  //@Parameter(names = {"--storage-db-version"}, description = "Storage db version.(1 or 2)")
  private String storageDbVersion = "";

  //@Parameter(names = {"--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
  private String storageDbEngine = "";

  //@Parameter(names = {"--storage-db-synchronous"}, description = "Storage db is synchronous or not.(true or false)")
  private String storageDbSynchronous = "";

  /*@Parameter(names = {
      "--contract-parse-enable"}, description = "enable contract parses in java-ledgerYi or not.(true or false)")*/
  private String contractParseEnable = "";

  //@Parameter(names = {"--storage-index-directory"}, description = "Storage index directory")
  private String storageIndexDirectory = "";

  //@Parameter(names = {"--storage-index-switch"}, description = "Storage index switch.(on or off)")
  private String storageIndexSwitch = "";

  /*@Parameter(names = {
      "--storage-transactionHistory-switch"}, description = "Storage transaction history switch.(on or off)")*/
  private String storageTransactionHistoreSwitch = "";

  @Getter
  //@Parameter(names = {"--fast-forward"})
  private boolean fastForward = false;

  @Getter
  private Storage storage;

  /*@Getter
  private Overlay overlay;*/

  @Getter
  private SeedNode seedNode;

  @Getter
  private GenesisBlock genesisBlock;

  @Getter
  @Setter
  private String chainId;

  @Getter
  @Setter
  private LocalMasters localMasters = new LocalMasters();

  @Getter
  @Setter
  private boolean needSyncCheck;

  @Getter
  @Setter
  private boolean nodeDiscoveryEnable;

  @Getter
  @Setter
  private boolean nodeDiscoveryPersist;

  @Getter
  @Setter
  private int nodeConnectionTimeout;

  @Getter
  @Setter
  private List<Node> activeNodes;

  @Getter
  @Setter
  private List<Node> passiveNodes;

  @Getter
  @Setter
  private List<Node> fastForwardNodes;

  @Getter
  @Setter
  private int nodeChannelReadTimeout;

  @Getter
  @Setter
  private int nodeMaxActiveNodes;

  @Getter
  @Setter
  private int nodeMaxActiveNodesWithSameIp;

  @Getter
  @Setter
  private int minParticipationRate;

  @Getter
  @Setter
  private int nodeListenPort;

  @Getter
  @Setter
  private String nodeDiscoveryBindIp;

  @Getter
  @Setter
  private String nodeExternalIp;

  @Getter
  @Setter
  private boolean nodeDiscoveryPublicHomeNode;

  @Getter
  @Setter
  private long nodeP2pPingInterval;

  @Getter
  @Setter
  //@Parameter(names = {"--save-internaltx"})
  private boolean saveInternalTx;

  @Getter
  @Setter
  private int nodeP2pVersion;

  @Getter
  @Setter
  private String p2pNodeId;

  @Getter
  @Setter
  private int rpcPort;

  @Getter
  @Setter
  private int ledgerYiNodeHttpPort;

  @Getter
  @Setter
  //@Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  private int rpcThreadNum;

  @Getter
  @Setter
  private int maxConcurrentCallsPerConnection;

  @Getter
  @Setter
  private int flowControlWindow;

  @Getter
  @Setter
  private long maxConnectionIdleInMillis;

  @Getter
  @Setter
  private int blockProducedTimeOut;

  @Getter
  @Setter
  private long netMaxTxPerSecond;

  @Getter
  @Setter
  private long maxConnectionAgeInMillis;

  @Getter
  @Setter
  private int maxMessageSize;

  @Getter
  @Setter
  private int maxHeaderListSize;

  @Getter
  @Setter
  //@Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  private int validateSignThreadNum;

  @Getter
  @Setter
  private int tcpNettyWorkThreadNum;

  @Getter
  @Setter
  private int udpNettyWorkThreadNum;

  @Getter
  @Setter
  private boolean walletExtensionApi;

  @Getter
  @Setter
  private int backupPriority;

  @Getter
  @Setter
  private int backupPort;

  @Getter
  @Setter
  private int keepAliveInterval;

  @Getter
  @Setter
  private List<String> backupMembers;

  @Getter
  @Setter
  private double connectFactor;

  @Getter
  @Setter
  private double activeConnectFactor;

  @Getter
  @Setter
  private double disconnectNumberFactor;

  @Getter
  @Setter
  private double maxConnectNumberFactor;

  @Getter
  @Setter
  private long receiveTcpMinDataLength;

  @Getter
  @Setter
  private boolean isOpenFullTcpDisconnect;

  @Getter
  @Setter
  private boolean vmTrace;

  @Getter
  @Setter
  private String txReferenceBlock;

  @Getter
  @Setter
  private int minEffectiveConnection;

  @Getter
  @Setter
  private String cryptoEngine = Constant.ECKey_ENGINE;

  @Getter
  @Setter
  private long txExpirationTimeInMilliseconds;

  @Getter
  private DbBackupConfig dbBackupConfig;

  @Getter
  private RocksDbSettings rocksDBCustomSettings;

  @Getter
  @Setter
  private int validContractProtoThreadNum;

  @Getter
  @Setter
  private RateLimiterInitialization rateLimiterInitialization;

  @Getter
  @Setter
  private Set<String> actuatorSet;

  @Getter
  @Setter
  public boolean ledgerYiNodeHttpEnable = true;

  @Getter
  @Setter
  public boolean isEcc = true;

  /**
   * set parameters.
   */
  public static void setParam(final String[] args, final String confFileName) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);

    Config config = Configuration.getByFileName(INSTANCE.shellConfFileName, confFileName);
    log.info("config file name: {}", confFileName);
    log.info("shell config file name: {}", INSTANCE.shellConfFileName);

    if (config.hasPath(Constant.CRYPTO_ENGINE)) {
      INSTANCE.isEcc = Constant.ECKey_ENGINE.equalsIgnoreCase(config.getString(Constant.CRYPTO_ENGINE));
    }
    if (config.hasPath(Constant.CRYPTO_ENGINE)) {
      INSTANCE.cryptoEngine = config.getString(Constant.CRYPTO_ENGINE);
    }
    initEncryptoEngine(INSTANCE);

    if (StringUtils.isNoneBlank(INSTANCE.privateKey)) {
      INSTANCE.setLocalMasters(new LocalMasters(INSTANCE.privateKey));
      if (StringUtils.isNoneBlank(INSTANCE.masterAddress)) {
        byte[] bytes = DecodeUtil.decode(INSTANCE.masterAddress);
        if (bytes != null) {
          INSTANCE.localMasters.setMasterAccountAddress(bytes);
          log.debug("Got localMasterAccountAddress from cmd");
        } else {
          INSTANCE.masterAddress = "";
          log.warn("The localMasterAccountAddress format is incorrect, ignored");
        }
      }
      INSTANCE.localMasters.initMasterAccountAddress(DBConfig.isEccCryptoEngine());
      log.debug("Got privateKey from cmd");
    } else if (config.hasPath(Constant.LOCAL_MASTER)) {
      INSTANCE.localMasters = new LocalMasters();
      List<String> localMaster = config.getStringList(Constant.LOCAL_MASTER);
      if (localMaster.size() > 1) {
        log.warn("localMaster size must be one, get the first one");
        localMaster = localMaster.subList(0, 1);
      }
      INSTANCE.localMasters.setPrivateKeys(localMaster);

      if (config.hasPath(Constant.LOCAL_MASTER_ACCOUNT_ADDRESS)) {
        byte[] bytes = DecodeUtil.decode(config.getString(Constant.LOCAL_MASTER_ACCOUNT_ADDRESS));
        if (bytes != null) {
          INSTANCE.localMasters.setMasterAccountAddress(bytes);
          log.debug("Got localMasterAccountAddress from config.conf");
        } else {
          log.warn("The localMasterAccountAddress format is incorrect, ignored");
        }
      }
      INSTANCE.localMasters.initMasterAccountAddress(DBConfig.isEccCryptoEngine());

      log.debug("Got privateKey from config.conf");
    }

    if (INSTANCE.isMaster() && CollectionUtils.isEmpty(INSTANCE.localMasters.getPrivateKeys())) {
      log.warn("This is a master node,but localMasters is null");
    }

    if (config.hasPath(Constant.NODE_HTTP_FULLNODE_ENABLE)) {
      INSTANCE.ledgerYiNodeHttpEnable = config.getBoolean(Constant.NODE_HTTP_FULLNODE_ENABLE);
    }

    INSTANCE.storage = new Storage();
    INSTANCE.storage.setDbVersion(Optional.ofNullable(INSTANCE.storageDbVersion)
        .filter(StringUtils::isNotEmpty)
        .map(Integer::valueOf)
        .orElse(Storage.getDbVersionFromConfig(config)));

    INSTANCE.storage.setDbEngine(Optional.ofNullable(INSTANCE.storageDbEngine)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbEngineFromConfig(config)));

    if (Constant.ROCKSDB.equals(INSTANCE.storage.getDbEngine().toUpperCase())
        && INSTANCE.storage.getDbVersion() == 1) {
      throw new RuntimeException("db.version = 1 is not supported by ROCKSDB engine.");
    }

    INSTANCE.storage.setDbSync(Optional.ofNullable(INSTANCE.storageDbSynchronous)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getDbVersionSyncFromConfig(config)));

    INSTANCE.storage.setContractParseSwitch(Optional.ofNullable(INSTANCE.contractParseEnable)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getContractParseSwitchFromConfig(config)));

    INSTANCE.storage.setDbDirectory(Optional.ofNullable(INSTANCE.storageDbDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbDirectoryFromConfig(config)));

    INSTANCE.storage.setIndexDirectory(Optional.ofNullable(INSTANCE.storageIndexDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexDirectoryFromConfig(config)));

    INSTANCE.storage.setIndexSwitch(Optional.ofNullable(INSTANCE.storageIndexSwitch)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexSwitchFromConfig(config)));

    INSTANCE.storage.setTransactionHistoreSwitch(Optional.ofNullable(INSTANCE.storageTransactionHistoreSwitch)
            .filter(StringUtils::isNotEmpty)
            .orElse(Storage.getTransactionHistoreSwitchFromConfig(config)));

    INSTANCE.storage.setPropertyMapFromConfig(config);

    INSTANCE.seedNode = new SeedNode();
    INSTANCE.seedNode.setIpList(Optional.ofNullable(INSTANCE.seedNodes)
        .filter(seedNode -> 0 != seedNode.size())
        .orElse(config.getStringList("seed.node.ip.list")));


    if (config.hasPath(Constant.GENESIS_BLOCK)) {
      INSTANCE.genesisBlock = new GenesisBlock();

      INSTANCE.genesisBlock.setTimestamp(config.getString(Constant.GENESIS_BLOCK_TIMESTAMP));
      INSTANCE.genesisBlock.setParentHash(config.getString(Constant.GENESIS_BLOCK_PARENTHASH));

      if (config.hasPath(Constant.GENESIS_BLOCK_ASSETS)) {
        INSTANCE.genesisBlock.setAssets(getAccountsFromConfig(config));
        AccountStore.setAccount(config);
      }
      if (config.hasPath(Constant.GENESIS_BLOCK_WITNESSES)) {
        INSTANCE.genesisBlock.setMasters(getMastersFromConfig(config));
      }
    } else {
      INSTANCE.genesisBlock = GenesisBlock.getDefault();
    }

    INSTANCE.needSyncCheck =
        config.hasPath(Constant.BLOCK_NEED_SYNC_CHECK) && config.getBoolean(Constant.BLOCK_NEED_SYNC_CHECK);

    INSTANCE.nodeDiscoveryEnable =
        config.hasPath(Constant.NODE_DISCOVERY_ENABLE) && config.getBoolean(Constant.NODE_DISCOVERY_ENABLE);

    INSTANCE.nodeDiscoveryPersist =
        config.hasPath(Constant.NODE_DISCOVERY_PERSIST) && config.getBoolean(Constant.NODE_DISCOVERY_PERSIST);

    INSTANCE.nodeConnectionTimeout =
        config.hasPath(Constant.NODE_CONNECTION_TIMEOUT) ? config.getInt(Constant.NODE_CONNECTION_TIMEOUT) * 1000 : 0;

    INSTANCE.nodeChannelReadTimeout =
        config.hasPath(Constant.NODE_CHANNEL_READ_TIMEOUT) ? config.getInt(Constant.NODE_CHANNEL_READ_TIMEOUT) : 0;

    INSTANCE.nodeMaxActiveNodes =
        config.hasPath(Constant.NODE_MAX_ACTIVE_NODES) ? config.getInt(Constant.NODE_MAX_ACTIVE_NODES) : 30;

    INSTANCE.nodeMaxActiveNodesWithSameIp =
        config.hasPath(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAMEIP) ? config.getInt(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAMEIP) : 2;

    INSTANCE.minParticipationRate =
        config.hasPath(Constant.NODE_MIN_PARTICIPATION_RATE) ? config.getInt(Constant.NODE_MIN_PARTICIPATION_RATE) : 0;

    INSTANCE.nodeListenPort =
        config.hasPath(Constant.NODE_LISTEN_PORT) ? config.getInt(Constant.NODE_LISTEN_PORT) : 0;

    bindIp(config);
    externalIp(config);

    INSTANCE.nodeDiscoveryPublicHomeNode =
        config.hasPath(Constant.NODE_DISCOVERY_PUBLIC_HOME_NODE) && config.getBoolean(Constant.NODE_DISCOVERY_PUBLIC_HOME_NODE);

    INSTANCE.nodeP2pPingInterval =
        config.hasPath(Constant.NODE_P2P_PING_INTERVAL) ? config.getLong(Constant.NODE_P2P_PING_INTERVAL) : 0;

    INSTANCE.nodeP2pVersion =
        config.hasPath(Constant.NODE_P2P_VERSION) ? config.getInt(Constant.NODE_P2P_VERSION) : 0;

    INSTANCE.rpcPort =
        config.hasPath(Constant.NODE_RPC_PORT) ? config.getInt(Constant.NODE_RPC_PORT) : 50051;

    INSTANCE.ledgerYiNodeHttpPort =
        config.hasPath(Constant.NODE_HTTP_FULLNODE_PORT) ? config.getInt(Constant.NODE_HTTP_FULLNODE_PORT) : 8090;

    INSTANCE.rpcThreadNum =
        config.hasPath(Constant.NODE_RPC_THREAD) ? config.getInt(Constant.NODE_RPC_THREAD)
            : Runtime.getRuntime().availableProcessors() / 2;

    INSTANCE.maxConcurrentCallsPerConnection =
        config.hasPath(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION) ?
            config.getInt(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION) : Integer.MAX_VALUE;

    INSTANCE.flowControlWindow = config.hasPath(Constant.NODE_RPC_FLOW_CONTROL_WINDOW) ?
        config.getInt(Constant.NODE_RPC_FLOW_CONTROL_WINDOW)
        : NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

    INSTANCE.maxConnectionIdleInMillis = config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS) ?
        config.getLong(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS) : Long.MAX_VALUE;

    INSTANCE.blockProducedTimeOut = config.hasPath(Constant.NODE_PRODUCED_TIMEOUT) ?
        config.getInt(Constant.NODE_PRODUCED_TIMEOUT) : BLOCK_PRODUCE_TIMEOUT_PERCENT;

    INSTANCE.maxHttpConnectNumber = config.hasPath(Constant.NODE_MAX_HTTP_CONNECT_NUMBER) ?
        config.getInt(Constant.NODE_MAX_HTTP_CONNECT_NUMBER) : NodeConstant.MAX_HTTP_CONNECT_NUMBER;

    if (INSTANCE.blockProducedTimeOut < 30) {
      INSTANCE.blockProducedTimeOut = 30;
    }
    if (INSTANCE.blockProducedTimeOut > 100) {
      INSTANCE.blockProducedTimeOut = 100;
    }

    INSTANCE.netMaxTxPerSecond = config.hasPath(Constant.NODE_NET_MAX_TX_PER_SECOND) ?
        config.getInt(Constant.NODE_NET_MAX_TX_PER_SECOND) : NetConstants.NET_MAX_TX_PER_SECOND;

    INSTANCE.maxConnectionAgeInMillis = config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS) ?
        config.getLong(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS) : Long.MAX_VALUE;

    INSTANCE.maxMessageSize = config.hasPath(Constant.NODE_RPC_MAX_MESSAGE_SIZE) ?
        config.getInt(Constant.NODE_RPC_MAX_MESSAGE_SIZE) : GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

    INSTANCE.maxHeaderListSize = config.hasPath(Constant.NODE_RPC_MAX_HEADER_LIST_ISZE) ?
        config.getInt(Constant.NODE_RPC_MAX_HEADER_LIST_ISZE) : GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;

    INSTANCE.tcpNettyWorkThreadNum = config.hasPath(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM) ? config
        .getInt(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM) : 0;

    INSTANCE.udpNettyWorkThreadNum = config.hasPath(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM) ? config
        .getInt(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM) : 1;

    INSTANCE.validateSignThreadNum = config.hasPath(Constant.NODE_VALIDATE_SIGN_THREAD_NUM) ?
            config.getInt(Constant.NODE_VALIDATE_SIGN_THREAD_NUM) : Runtime.getRuntime().availableProcessors() / 2;

    INSTANCE.walletExtensionApi = config.hasPath(Constant.NODE_WALLET_EXTENSION_API) && config.getBoolean(Constant.NODE_WALLET_EXTENSION_API);

    INSTANCE.connectFactor =  config.hasPath(Constant.NODE_CONNECT_FACTOR) ? config.getDouble(Constant.NODE_CONNECT_FACTOR) : 0.3;

    INSTANCE.activeConnectFactor = config.hasPath(Constant.NODE_ACTIVE_CONNECT_FACTOR) ?
        config.getDouble(Constant.NODE_ACTIVE_CONNECT_FACTOR) : 0.1;

    INSTANCE.disconnectNumberFactor = config.hasPath(Constant.NODE_DISCONNECT_NUMBER_FACTOR) ?
        config.getDouble(Constant.NODE_DISCONNECT_NUMBER_FACTOR) : 0.4;
    INSTANCE.maxConnectNumberFactor = config.hasPath(Constant.NODE_MAX_CONNECT_NUMBER_FACTOR) ?
        config.getDouble(Constant.NODE_MAX_CONNECT_NUMBER_FACTOR) : 0.8;
    INSTANCE.receiveTcpMinDataLength = config.hasPath(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH) ?
        config.getLong(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH) : 2048;

    INSTANCE.isOpenFullTcpDisconnect = config.hasPath(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT) && config
        .getBoolean(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT);

    INSTANCE.txReferenceBlock = config.hasPath(Constant.TX_REFERENCE_BLOCK) ?
        config.getString(Constant.TX_REFERENCE_BLOCK) : "head";

    INSTANCE.txExpirationTimeInMilliseconds =
        config.hasPath(Constant.TX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            && config.getLong(Constant.TX_EXPIRATION_TIME_IN_MILLIS_SECONDS) > 0 ?
            config.getLong(Constant.TX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            : Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;

    INSTANCE.minEffectiveConnection = config.hasPath(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION) ?
        config.getInt(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION) : 1;

    INSTANCE.validContractProtoThreadNum = config.hasPath(Constant.NODE_VALID_CONTRACT_PROTO_THREADS) ? config
            .getInt(Constant.NODE_VALID_CONTRACT_PROTO_THREADS)
            : Runtime.getRuntime().availableProcessors();

    INSTANCE.activeNodes = getNodes(config, Constant.NODE_ACTIVE);

    INSTANCE.passiveNodes = getNodes(config, Constant.NODE_PASSIVE);

    INSTANCE.fastForwardNodes = getNodes(config, Constant.NODE_FAST_FORWARD);

    INSTANCE.rateLimiterInitialization = config.hasPath(Constant.RATE_LIMITER) ? getRateLimiterFromConfig(config)
            : new RateLimiterInitialization();

    initBackupProperty(config);
    if (Constant.ROCKSDB.equals(Args.getInstance().getStorage().getDbEngine().toUpperCase())) {
      initRocksDbBackupProperty(config);
      initRocksDbSettings(config);
    }

    INSTANCE.actuatorSet = config.hasPath(Constant.ACTUATOR_WHITELIST)
            ? new HashSet<>(config.getStringList(Constant.ACTUATOR_WHITELIST)) : Collections.emptySet();

    logConfig();
    initDBConfig(INSTANCE);
  }

  private static List<Master> getMastersFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList(Constant.GENESIS_BLOCK_WITNESSES).stream()
        .map(Args::createMaster).collect(Collectors.toCollection(ArrayList::new));
  }

  private static Master createMaster(final ConfigObject masterAccount) {
    final Master master = new Master();
    master.setAddress(DecodeUtil.decode(masterAccount.get("address").unwrapped().toString()));
    return master;
  }

  private static List<Account> getAccountsFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList(Constant.GENESIS_BLOCK_ASSETS).stream()
        .map(Args::createAccount).collect(Collectors.toCollection(ArrayList::new));
  }

  private static Account createAccount(final ConfigObject asset) {
    final Account account = new Account();
    account.setAccountName(asset.get("accountName").unwrapped().toString());
    account.setAccountType(asset.get("accountType").unwrapped().toString());
    account.setAddress(DecodeUtil.decode(asset.get("address").unwrapped().toString()));
    return account;
  }

  private static RateLimiterInitialization getRateLimiterFromConfig(final com.typesafe.config.Config config) {
    RateLimiterInitialization initialization = new RateLimiterInitialization();
    ArrayList<RateLimiterInitialization.HttpRateLimiterItem> list1 = config
        .getObjectList("rate.limiter.http").stream()
        .map(RateLimiterInitialization::createHttpItem)
        .collect(Collectors.toCollection(ArrayList::new));
    initialization.setHttpMap(list1);

    ArrayList<RateLimiterInitialization.RpcRateLimiterItem> list2 = config
        .getObjectList("rate.limiter.rpc").stream()
        .map(RateLimiterInitialization::createRpcItem)
        .collect(Collectors.toCollection(ArrayList::new));

    initialization.setRpcMap(list2);
    return initialization;
  }

  public static Args getInstance() {
    return INSTANCE;
  }

  private static List<Node> getNodes(final com.typesafe.config.Config config, String path) {
    if (!config.hasPath(path)) {
      return Collections.emptyList();
    }
    List<Node> ret = new ArrayList<>();
    List<String> list = config.getStringList(path);
    for (String configString : list) {
      Node n = Node.instanceOf(configString);
      if (!(INSTANCE.nodeDiscoveryBindIp.equals(n.getHost()) ||
          INSTANCE.nodeExternalIp.equals(n.getHost()) ||
          "127.0.0.1".equals(n.getHost())) ||
          INSTANCE.nodeListenPort != n.getPort()) {
        ret.add(n);
      }
    }
    return ret;
  }

  private static void bindIp(final com.typesafe.config.Config config) {
    if (!config.hasPath(Constant.NODE_DISCOVERY_BIND_IP) || config.getString(Constant.NODE_DISCOVERY_BIND_IP).trim().isEmpty()) {
      if (INSTANCE.nodeDiscoveryBindIp == null) {
        log.info("Bind address wasn't set, Punching to identify it...");
        try (Socket s = new Socket("www.baidu.com", 80)) {
          INSTANCE.nodeDiscoveryBindIp = s.getLocalAddress().getHostAddress();
          log.info("UDP local bound to: {}", INSTANCE.nodeDiscoveryBindIp);
        } catch (IOException e) {
          log.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
          INSTANCE.nodeDiscoveryBindIp = "0.0.0.0";
        }
      }
    } else {
      INSTANCE.nodeDiscoveryBindIp = config.getString(Constant.NODE_DISCOVERY_BIND_IP).trim();
    }
  }

  private static void externalIp(final com.typesafe.config.Config config) {
    if (!config.hasPath(Constant.NODE_DISCOVERY_EXTENNAL_IP) || config.getString(Constant.NODE_DISCOVERY_EXTENNAL_IP).trim().isEmpty()) {
      if (INSTANCE.nodeExternalIp == null) {
        log.info("External IP wasn't set, using checkip.baidu.com to identify it...");
        BufferedReader in = null;
        try {
          in = new BufferedReader(new InputStreamReader( new URL(Constant.BAUDU_URL).openStream()));
          INSTANCE.nodeExternalIp = in.readLine();
          if (INSTANCE.nodeExternalIp == null || INSTANCE.nodeExternalIp.trim().isEmpty()) {
            throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
          }
          try {
            InetAddress.getByName(INSTANCE.nodeExternalIp);
          } catch (Exception e) {
            throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
          }
          log.info("External address identified: {}", INSTANCE.nodeExternalIp);
        } catch (IOException e) {
          INSTANCE.nodeExternalIp = INSTANCE.nodeDiscoveryBindIp;
          log.warn("Can't get external IP. Fall back to peer.bind.ip: " + INSTANCE.nodeExternalIp + " :" + e);
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              //ignore
            }
          }
        }
      }
    } else {
      INSTANCE.nodeExternalIp = config.getString(Constant.NODE_DISCOVERY_EXTENNAL_IP).trim();
    }
  }

  private static void initRocksDbSettings(Config config) {
    String prefix = "storage.dbSettings.";
    int levelNumber = config.hasPath(prefix + "levelNumber")
            ? config.getInt(prefix + "levelNumber") : 7;

    int compactThreads = config.hasPath(prefix + "compactThreads")
        ? config.getInt(prefix + "compactThreads") : max(Runtime.getRuntime().availableProcessors(), 1);

    int blockSize = config.hasPath(prefix + "blockSize")
        ? config.getInt(prefix + "blockSize") : 16;

    long maxBytesForLevelBase = config.hasPath(prefix + "maxBytesForLevelBase")
        ? config.getInt(prefix + "maxBytesForLevelBase") : 256;

    double maxBytesForLevelMultiplier = config.hasPath(prefix + "maxBytesForLevelMultiplier")
        ? config.getDouble(prefix + "maxBytesForLevelMultiplier") : 10;

    int level0FileNumCompactionTrigger =
        config.hasPath(prefix + "level0FileNumCompactionTrigger")
                ? config.getInt(prefix + "level0FileNumCompactionTrigger") : 2;

    long targetFileSizeBase = config.hasPath(prefix + "targetFileSizeBase")
            ? config.getLong(prefix + "targetFileSizeBase") : 64;

    int targetFileSizeMultiplier = config.hasPath(prefix + "targetFileSizeMultiplier")
            ? config.getInt(prefix + "targetFileSizeMultiplier") : 1;

    INSTANCE.rocksDBCustomSettings = RocksDbSettings.initCustomSettings(
            levelNumber, compactThreads,
            blockSize, maxBytesForLevelBase,
            maxBytesForLevelMultiplier, level0FileNumCompactionTrigger,
            targetFileSizeBase, targetFileSizeMultiplier);
    RocksDbSettings.loggingSettings();
  }

  private static void initRocksDbBackupProperty(Config config) {
    boolean enable = config.hasPath(Constant.STORAGE_BACKUP_ENABLE) && config.getBoolean(Constant.STORAGE_BACKUP_ENABLE);
    String propPath = config.hasPath(Constant.STORAGE_BACKUP_PROP_PATH)
        ? config.getString(Constant.STORAGE_BACKUP_PROP_PATH) : "prop.properties";
    String bak1path = config.hasPath("storage.backup.bak1path")
        ? config.getString("storage.backup.bak1path") : "bak1/database/";
    String bak2path = config.hasPath("storage.backup.bak2path")
        ? config.getString("storage.backup.bak2path") : "bak2/database/";
    int frequency = config.hasPath("storage.backup.frequency")
        ? config.getInt("storage.backup.frequency") : 10000;
    INSTANCE.dbBackupConfig = DbBackupConfig.getInstance()
        .initArgs(enable, propPath, bak1path, bak2path, frequency);
  }

  private static void initBackupProperty(Config config) {
    INSTANCE.backupPriority = config.hasPath(Constant.NODE_BACKUP_PRIORITY)
        ? config.getInt(Constant.NODE_BACKUP_PRIORITY) : 0;
    INSTANCE.backupPort = config.hasPath(Constant.NODE_BACKUP_PORT)
        ? config.getInt(Constant.NODE_BACKUP_PORT) : 10001;
    INSTANCE.keepAliveInterval = config.hasPath(Constant.NODE_BACKUP_KEEPALIVE_INTERVAL)
            ? config.getInt(Constant.NODE_BACKUP_KEEPALIVE_INTERVAL) : 3000;
    INSTANCE.backupMembers = config.hasPath(Constant.NODE_BACKUP_MEMBERS)
        ? config.getStringList(Constant.NODE_BACKUP_MEMBERS) : new ArrayList<>();
  }

  private static void logConfig() {
    Args args = getInstance();
    log.info("\n");
    log.info("************************ Node config ************************");
    log.info("Is master node: {}", args.isMaster());
    log.info("************************ Net config ************************");
    log.info("P2P version: {}", args.getNodeP2pVersion());
    log.info("Bind IP: {}", args.getNodeDiscoveryBindIp());
    log.info("External IP: {}", args.getNodeExternalIp());
    log.info("Listen port: {}", args.getNodeListenPort());
    log.info("Discover enable: {}", args.isNodeDiscoveryEnable());
    log.info("Active node size: {}", args.getActiveNodes().size());
    log.info("Passive node size: {}", args.getPassiveNodes().size());
    log.info("FastForward node size: {}", args.getFastForwardNodes().size());
    log.info("Seed node size: {}", args.getSeedNode().getIpList().size());
    log.info("Max connection: {}", args.getNodeMaxActiveNodes());
    log.info("Max connection with same IP: {}", args.getNodeMaxActiveNodesWithSameIp());
    log.info("************************ Backup config ************************");
    log.info("Backup listen port: {}", args.getBackupPort());
    log.info("Backup member size: {}", args.getBackupMembers().size());
    log.info("Backup priority: {}", args.getBackupPriority());
    log.info("************************ DB config *************************");
    log.info("DB version : {}", args.getStorage().getDbVersion());
    log.info("DB engine : {}", args.getStorage().getDbEngine());
    log.info("***************************************************************");
    log.info("\n");
  }

  public static void initEncryptoEngine(Args cfgArgs) {
    DBConfig.setEccCryptoEngine(cfgArgs.isEcc());
  }

  public static void initDBConfig(Args cfgArgs) {
    if (Objects.nonNull(cfgArgs.getStorage())) {
      DBConfig.setDbVersion(cfgArgs.getStorage().getDbVersion());
      DBConfig.setDbEngine(cfgArgs.getStorage().getDbEngine());
      DBConfig.setPropertyMap(cfgArgs.getStorage().getPropertyMap());
      DBConfig.setDbSync(cfgArgs.getStorage().isDbSync());
      DBConfig.setDbDirectory(cfgArgs.getStorage().getDbDirectory());
    }
    if (Objects.nonNull(cfgArgs.getGenesisBlock())) {
      DBConfig.setBlockTimestamp(cfgArgs.getGenesisBlock().getTimestamp());
      DBConfig.setGenesisBlock(cfgArgs.getGenesisBlock());
    }
    DBConfig.setOutputDirectoryConfig(cfgArgs.getOutputDirectory());
    DBConfig.setRocksDbSettings(cfgArgs.getRocksDBCustomSettings());
    DBConfig.setValidContractProtoThreadNum(cfgArgs.getValidContractProtoThreadNum());
    DBConfig.setDebug(cfgArgs.isDebug());
    DBConfig.setActuatorSet(cfgArgs.getActuatorSet());
    DBConfig.setEccCryptoEngine(cfgArgs.isEcc());
  }

  /**
   * Get storage path by name of database
   *
   * @param dbName name of database
   * @return path of that database
   */
  public String getOutputDirectoryByDbName(String dbName) {
    String path = storage.getPathByDbName(dbName);
    if (!StringUtils.isBlank(path)) {
      return path;
    }
    return getOutputDirectory();
  }

  /**
   * get output directory.
   */
  public String getOutputDirectory() {
    if (!this.outputDirectory.equals("") && !this.outputDirectory.endsWith(File.separator)) {
      return this.outputDirectory + File.separator;
    }
    return this.outputDirectory;
  }

  public boolean isEccCryptoEngine() {
    return isEcc;
  }
}