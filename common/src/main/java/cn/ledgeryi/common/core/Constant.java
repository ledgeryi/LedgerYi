package cn.ledgeryi.common.core;

public class Constant {

  //config for mainnet
  public static final String MAINNET_CONF = "config.conf";

  // config for transaction
  public static final long TRANSACTION_MAX_BYTE_SIZE = 500 * 1_024L;
  public static final long MAXIMUM_TIME_UNTIL_EXPIRATION = 24 * 60 * 60 * 1_000L; //one day
  public static final long TRANSACTION_DEFAULT_EXPIRATION_TIME = 60 * 1_000L; //60 seconds

  // Configuration items
  public static final String LOCAL_MASTER = "localMaster";
  public static final String LOCAL_MASTER_ACCOUNT_ADDRESS = "localMasterAccountAddress";
  public static final String VM_LONG_RUNNING_TIME = "vm.longRunningTime";

  public static final String ROCKSDB = "ROCKSDB";

  public static final String GENESIS_BLOCK = "genesis.block";
  public static final String GENESIS_BLOCK_TIMESTAMP = "genesis.block.timestamp";
  public static final String GENESIS_BLOCK_PARENTHASH = "genesis.block.parentHash";
  public static final String GENESIS_BLOCK_ASSETS = "genesis.block.assets";
  public static final String GENESIS_BLOCK_WITNESSES = "genesis.block.masters";

  public static final String BLOCK_NEED_SYNC_CHECK = "block.needSyncCheck";
  public static final String NODE_DISCOVERY_ENABLE = "node.discovery.enable";
  public static final String NODE_DISCOVERY_PERSIST = "node.discovery.persist";
  public static final String NODE_CONNECTION_TIMEOUT = "node.connection.timeout";
  public static final String NODE_CHANNEL_READ_TIMEOUT = "node.channel.read.timeout";
  public static final String NODE_MAX_ACTIVE_NODES = "node.maxActiveNodes";
  public static final String NODE_MAX_ACTIVE_NODES_WITH_SAMEIP = "node.maxActiveNodesWithSameIp";
  public static final String NODE_MIN_PARTICIPATION_RATE = "node.minParticipationRate";
  public static final String NODE_LISTEN_PORT = "node.listen.port";
  public static final String NODE_DISCOVERY_PUBLIC_HOME_NODE = "node.discovery.public.home.node";

  public static final String NODE_P2P_PING_INTERVAL = "node.p2p.pingInterval";
  public static final String NODE_P2P_VERSION = "node.p2p.version";
  public static final String NODE_RPC_PORT = "node.rpc.port";
  public static final String NODE_HTTP_FULLNODE_PORT = "node.http.ledgerYiNodePort";
  public static final String NODE_HTTP_FULLNODE_ENABLE = "node.http.ledgerYiNodeEnable";
  public static final String NODE_RPC_THREAD = "node.rpc.thread";
  public static final String NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION = "node.rpc.maxConcurrentCallsPerConnection";
  public static final String NODE_RPC_FLOW_CONTROL_WINDOW = "node.rpc.flowControlWindow";
  public static final String NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS = "node.rpc.maxConnectionIdleInMillis";
  public static final String NODE_PRODUCED_TIMEOUT = "node.blockProducedTimeOut";
  public static final String NODE_MAX_HTTP_CONNECT_NUMBER = "node.maxHttpConnectNumber";
  public static final String NODE_NET_MAX_TX_PER_SECOND = "node.netMaxTxPerSecond";
  public static final String NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS = "node.rpc.maxConnectionAgeInMillis";
  public static final String NODE_RPC_MAX_MESSAGE_SIZE = "node.rpc.maxMessageSize";
  public static final String NODE_RPC_MAX_HEADER_LIST_ISZE = "node.rpc.maxHeaderListSize";
  public static final String NODE_TCP_NETTY_WORK_THREAD_NUM = "node.tcpNettyWorkThreadNum";
  public static final String NODE_UDP_NETTY_WORK_THREAD_NUM = "node.udpNettyWorkThreadNum";
  public static final String NODE_VALIDATE_SIGN_THREAD_NUM = "node.validateSignThreadNum";
  public static final String NODE_WALLET_EXTENSION_API = "node.walletExtensionApi";
  public static final String NODE_CONNECT_FACTOR = "node.connectFactor";
  public static final String NODE_ACTIVE_CONNECT_FACTOR = "node.activeConnectFactor";
  public static final String NODE_DISCONNECT_NUMBER_FACTOR = "node.disconnectNumberFactor";
  public static final String NODE_MAX_CONNECT_NUMBER_FACTOR = "node.maxConnectNumberFactor";
  public static final String NODE_RECEIVE_TCP_MIN_DATA_LENGTH = "node.receiveTcpMinDataLength";
  public static final String NODE_IS_OPEN_FULL_TCP_DISCONNECT = "node.isOpenFullTcpDisconnect";
  public static final String NODE_RPC_MIN_EFFECTIVE_CONNECTION = "node.rpc.minEffectiveConnection";
  public static final String NODE_VALID_CONTRACT_PROTO_THREADS = "node.validContractProto.threads";
  public static final String NODE_DISCOVERY_BIND_IP = "node.discovery.bind.ip";
  public static final String NODE_DISCOVERY_EXTENNAL_IP = "node.discovery.external.ip";
  public static final String NODE_BACKUP_PRIORITY = "node.backup.priority";
  public static final String NODE_BACKUP_PORT = "node.backup.port";
  public static final String NODE_BACKUP_KEEPALIVE_INTERVAL = "node.backup.keepAliveInterval";
  public static final String NODE_BACKUP_MEMBERS = "node.backup.members";

  public static final String STORAGE_NEEDTO_UPDATE_ASSET = "storage.needToUpdateAsset";

  public static final String  TX_REFERENCE_BLOCK = "tx.reference.block";
  public static final String TX_EXPIRATION_TIME_IN_MILLIS_SECONDS = "tx.expiration.timeInMilliseconds";


  public static final String NODE_ACTIVE = "node.active";
  public static final String NODE_PASSIVE = "node.passive";
  public static final String NODE_FAST_FORWARD = "node.fastForward";

  public static final String RATE_LIMITER = "rate.limiter";

  public static final String CRYPTO_ENGINE = "crypto.engine";
  public static final String ECKey_ENGINE = "ECKey";

  public static final String BAUDU_URL = "http://www.baidu.com";

  public static final String STORAGE_BACKUP_ENABLE = "storage.backup.enable";
  public static final String STORAGE_BACKUP_PROP_PATH = "storage.backup.propPath";

  public static final String ACTUATOR_WHITELIST = "actuator.whitelist";
}
