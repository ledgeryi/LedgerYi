package cn.ledgeryi.framework.core.db;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.ChainBaseManager;
import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;
import cn.ledgeryi.chainbase.core.config.args.Master;
import cn.ledgeryi.chainbase.core.db.*;
import cn.ledgeryi.chainbase.core.db2.core.ILedgerYiBase;
import cn.ledgeryi.chainbase.core.db2.core.ISession;
import cn.ledgeryi.chainbase.core.db2.core.SnapshotManager;
import cn.ledgeryi.chainbase.core.store.*;
import cn.ledgeryi.common.core.Constant;
import cn.ledgeryi.common.core.exception.*;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.Pair;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.consenus.Consensus;
import cn.ledgeryi.consenus.base.Param;
import cn.ledgeryi.contract.utils.TransactionRegister;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.runtime.RuntimeImpl;
import cn.ledgeryi.framework.common.utils.BlockUtil;
import cn.ledgeryi.framework.common.utils.ForkController;
import cn.ledgeryi.framework.common.utils.SessionOptional;
import cn.ledgeryi.framework.core.actuator.ActuatorCreator;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.exception.*;
import cn.ledgeryi.framework.core.net.LedgerYiNetService;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction.Contract;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static cn.ledgeryi.chainbase.core.config.Parameter.NodeConstant.MAX_TRANSACTION_PENDING;

@Slf4j(topic = "DB")
@Component
public class Manager {

    @Getter
    @Autowired
    private AccountStore accountStore;
    @Autowired
    private TransactionStore transactionStore;
    @Autowired
    private BlockStore blockStore;
    @Autowired
    private MasterStore masterStore;
    @Autowired
    private DynamicPropertiesStore dynamicPropertiesStore;
    @Autowired
    @Getter
    private BlockIndexStore blockIndexStore;
    @Autowired
    @Getter
    private TransactionRetStore transactionRetStore;
    @Autowired
    private MasterScheduleStore masterScheduleStore;
    @Autowired
    private RecentBlockStore recentBlockStore;
    @Autowired
    private TransactionHistoryStore transactionHistoryStore;
    @Autowired
    private CodeStore codeStore;
    @Autowired
    private ContractStore contractStore;
    @Autowired
    @Getter
    private StorageRowStore storageRowStore;
    @Setter
    private LedgerYiNetService ledgerYiNetService;
    @Autowired
    private PeersStore peersStore;
    @Autowired
    private KhaosDatabase khaosDb;
    private BlockCapsule genesisBlock;
    @Getter
    @Autowired
    private RevokingDatabase revokingStore;
    @Getter
    private SessionOptional session = SessionOptional.instance();
    @Getter
    @Setter
    private boolean isSyncMode;
    @Getter
    @Setter
    private String netType;
    @Autowired
    private Consensus consensus;
    @Autowired
    @Getter
    private ChainBaseManager chainBaseManager;
    @Getter
    private final List<TransactionCapsule> popedTransactions = Collections.synchronizedList(Lists.newArrayList());
    @Getter
    private final Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder.newBuilder().maximumSize(100_000).recordStats().build();
    @Getter
    private final ForkController forkController = ForkController.instance();
    private ExecutorService validateSignService;
    private boolean isRunRepushThread = true;
    private final Set<String> ownerAddressSet = new HashSet<>();
    private List<TransactionCapsule> pendingTransactions;
    private final BlockingQueue<TransactionCapsule> pushTransactionQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<TransactionCapsule> repushTransactions; // the capacity is equal to Integer.MAX_VALUE default

    /**
     * Cycle thread to repush Transactions
     */
    private Runnable repushLoop =
            () -> {
                while (isRunRepushThread) {
                    TransactionCapsule tx = null;
                    try {
                        tx = getRepushTransactions().peek();
                        if (tx != null) {
                            this.rePush(tx);
                        } else {
                            TimeUnit.MILLISECONDS.sleep(50L);
                        }
                    } catch (Exception ex) {
                        log.error("unknown exception happened in repush loop", ex);
                    } catch (Throwable throwable) {
                        log.error("unknown throwable happened in repush loop", throwable);
                    } finally {
                        if (tx != null) {
                            getRepushTransactions().remove(tx);
                        }
                    }
                }
            };

    public MasterStore getMasterStore() {
        return this.masterStore;
    }

    public DynamicPropertiesStore getDynamicPropertiesStore() {
        return this.dynamicPropertiesStore;
    }

    public MasterScheduleStore getMasterScheduleStore() {
        return this.masterScheduleStore;
    }

    private List<TransactionCapsule> getPendingTransactions() {
        return this.pendingTransactions;
    }

    public List<TransactionCapsule> getPoppedTransactions() {
        return this.popedTransactions;
    }

    public BlockingQueue<TransactionCapsule> getRepushTransactions() {
        return repushTransactions;
    }

  /*public BlockCapsule getHead() throws HeaderNotFound {
    List<BlockCapsule> blocks = getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isNotEmpty(blocks)) {
      return blocks.get(0);
    } else {
      log.info("Header block Not Found");
      throw new HeaderNotFound("Header block Not Found");
    }
  }*/

    public synchronized BlockCapsule.BlockId getHeadBlockId() {
        return new BlockCapsule.BlockId(
                getDynamicPropertiesStore().getLatestBlockHeaderHash(), getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    }

    public long getHeadBlockNum() {
        return getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    }

    public long getHeadBlockTimeStamp() {
        return getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    }

    public void clearAndWriteNeighbours(Set<Node> nodes) {
        this.peersStore.put("neighbours".getBytes(), nodes);
    }

    public Set<Node> readNeighbours() {
        return this.peersStore.get("neighbours".getBytes());
    }

    public void stopRepushThread() {
        isRunRepushThread = false;
    }

    public ContractStore getContractStore() {
        return contractStore;
    }

    public CodeStore getCodeStore() {
        return codeStore;
    }

    @PostConstruct
    public void init() {
        Message.setDynamicPropertiesStore(this.getDynamicPropertiesStore());
        revokingStore.disable();
        revokingStore.check();
        this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
        this.repushTransactions = new LinkedBlockingQueue<>();

        this.initGenesis();
        try {
            this.khaosDb.start(getBlockById(getDynamicPropertiesStore().getLatestBlockHeaderHash()));
        } catch (ItemNotFoundException e) {
            log.error("Can not find Dynamic highest block from DB! \nnumber={} \nhash={}",
                    getDynamicPropertiesStore().getLatestBlockHeaderNumber(), getDynamicPropertiesStore().getLatestBlockHeaderHash());
            log.error("Please delete database directory({}) and restart", Args.getInstance().getOutputDirectory());
            System.exit(1);
        } catch (BadItemException e) {
            e.printStackTrace();
            log.error("DB data broken! Please delete database directory({}) and restart", Args.getInstance().getOutputDirectory());
            System.exit(1);
        }
        forkController.init(this);

        revokingStore.enable();
        validateSignService = Executors.newFixedThreadPool(Args.getInstance().getValidateSignThreadNum());

        Thread repushThread = new Thread(repushLoop);
        repushThread.start();

        //initStoreFactory
        prepareStoreFactory();
        //initActuatorCreator
        ActuatorCreator.init();
        TransactionRegister.registerActuator();
    }

    public BlockCapsule.BlockId getGenesisBlockId() {
        return this.genesisBlock.getBlockId();
    }

    public BlockCapsule getGenesisBlock() {
        return genesisBlock;
    }

    /**
     * init genesis block.
     */
    private void initGenesis() {
        this.genesisBlock = BlockUtil.newGenesisBlockCapsule();
        if (this.containBlock(this.genesisBlock.getBlockId())) {
            Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());
        } else {
            if (this.hasBlocks()) {
                log.error("genesis block modify, please delete database directory({}) and restart", Args.getInstance().getOutputDirectory());
                System.exit(1);
            } else {
                log.info("create genesis block");
                Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());
                blockStore.put(this.genesisBlock.getBlockId().getBytes(), this.genesisBlock);
                this.blockIndexStore.put(this.genesisBlock.getBlockId());

                log.info("save block: " + this.genesisBlock);
                // init DynamicPropertiesStore
                this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(0);
                this.dynamicPropertiesStore.saveLatestBlockHeaderHash(this.genesisBlock.getBlockId().getByteString());
                this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(this.genesisBlock.getTimeStamp());
                this.initMaster();
                this.khaosDb.start(genesisBlock);
                this.updateRecentBlock(genesisBlock);
            }
        }
    }

    /**
     * save masters into database.
     */
    private void initMaster() {
        final Args args = Args.getInstance();
        final GenesisBlock genesisBlockArg = args.getGenesisBlock();
        genesisBlockArg.getMasters().forEach(this::addMaster);
    }

    public void addMaster(Master master) {
        byte[] keyAddress = master.getAddress();
        ByteString address = ByteString.copyFrom(keyAddress);
        final AccountCapsule accountCapsule;
        if (!this.accountStore.has(keyAddress)) {
            accountCapsule = new AccountCapsule(address, Protocol.AccountType.AssetIssue, 0L);
        } else {
            accountCapsule = this.accountStore.getUnchecked(keyAddress);
        }

        //add to account storage
        accountCapsule.setIsMaster(true);
        this.accountStore.put(keyAddress, accountCapsule);

        //add to master storage
        if (!masterStore.has(keyAddress)){
            final MasterCapsule masterCapsule = new MasterCapsule(address);
            this.masterStore.put(keyAddress, masterCapsule);
        }

    }

    private void validateTapos(TransactionCapsule transactionCapsule) throws TaposException {
        byte[] refBlockHash = transactionCapsule.getInstance().getRawData().getRefBlockHash().toByteArray();
        byte[] refBlockNumBytes = transactionCapsule.getInstance().getRawData().getRefBlockBytes().toByteArray();
        try {
            byte[] blockHash = this.recentBlockStore.get(refBlockNumBytes).getData();
            if (!Arrays.equals(blockHash, refBlockHash)) {
                String str = String.format("Tapos failed, different block hash, %s, %s , recent block %s, solid block %s head block %s",
                        ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash), Hex.toHexString(blockHash),
                        getSolidBlockId().getString(), getHeadBlockId().getString());
                log.info(str);
                throw new TaposException(str);
            }
        } catch (ItemNotFoundException e) {
            String str = String.format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
                    ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash), getSolidBlockId().getString(), getHeadBlockId().getString());
            log.info(str);
            throw new TaposException(str);
        }
    }

    private void validateCommon(TransactionCapsule transactionCapsule) throws TransactionExpirationException, TooBigTransactionException, ValidateSignatureException {
        if (transactionCapsule.getData().length > Constant.TRANSACTION_MAX_BYTE_SIZE) {
            throw new TooBigTransactionException("too big transaction, the size is " + transactionCapsule.getData().length + " bytes");
        }
        long transactionExpiration = transactionCapsule.getExpiration();
        long headBlockTime = getHeadBlockTimeStamp();
        if (transactionExpiration < headBlockTime) {
            throw new TransactionExpirationException("transaction expiration, transaction expiration time is "
                    + transactionExpiration + ", but headBlockTime is " + headBlockTime);
        }
        if (!transactionCapsule.validateSignature()) {
            throw new ValidateSignatureException("trans sig validate failed");
        }
    }

    private void validateDup(TransactionCapsule transactionCapsule) throws DupTransactionException {
        if (containsTransaction(transactionCapsule)) {
            log.error(ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes()));
            throw new DupTransactionException("dup trans");
        }
    }

    private boolean containsTransaction(TransactionCapsule transactionCapsule) {
        return transactionStore.has(transactionCapsule.getTransactionId().getBytes());
    }

    /**
     * push transaction into pending.
     */
    public boolean pushTransaction(final TransactionCapsule tx)
            throws ValidateSignatureException, ContractValidateException, DupTransactionException, TaposException,
            TooBigTransactionException, TransactionExpirationException, ReceiptCheckErrException, ContractExeException, VMIllegalException {

        synchronized (pushTransactionQueue) {
            pushTransactionQueue.add(tx);
        }

        try {
            if (!tx.validateSignature()) {
                throw new ValidateSignatureException("trans sig validate failed");
            }

            synchronized (this) {
                if (!session.valid()) {
                    session.setValue(revokingStore.buildSession());
                }

                try (ISession tmpSession = revokingStore.buildSession()) {
                    processTransaction(tx, null);
                    pendingTransactions.add(tx);
                    tmpSession.merge();
                } catch (Exception e) {
                    log.error("process tx error, error: ", e);
                    throw e;
                }
            }
        } finally {
            pushTransactionQueue.remove(tx);
        }
        log.info("Verify transaction Success. Hash:{}, From:{} Will be broadcast transaction",
                tx.getTransactionId(), ByteUtil.toHexString(TransactionCapsule.getOwnerAddress(tx)));
        return true;
    }

    /**
     * when switch fork need erase blocks on fork branch.
     */
    private synchronized void eraseBlock() {
        session.reset();
        try {
            BlockCapsule oldHeadBlock = getBlockById(getDynamicPropertiesStore().getLatestBlockHeaderHash());
            log.info("begin to erase block:" + oldHeadBlock);
            khaosDb.pop();
            revokingStore.fastPop();
            log.info("end to erase block:" + oldHeadBlock);
            popedTransactions.addAll(oldHeadBlock.getTransactions());
        } catch (ItemNotFoundException | BadItemException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void applyBlock(BlockCapsule block) throws ContractValidateException,
            ContractExeException, ValidateSignatureException, TransactionExpirationException,
            TooBigTransactionException, DupTransactionException, TaposException, ValidateScheduleException,
            ReceiptCheckErrException, BadBlockException, VMIllegalException {

        processBlock(block);
        this.blockStore.put(block.getBlockId().getBytes(), block);
        this.blockIndexStore.put(block.getBlockId());
        if (block.getTransactions().size() != 0) {
            this.transactionRetStore.put(ByteArray.fromLong(block.getNum()), block.getResult());
        }

        updateFork(block);
        if (System.currentTimeMillis() - block.getTimeStamp() >= 60_000) {
            revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MAX_FLUSH_COUNT);
        } else {
            revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MIN_FLUSH_COUNT);
        }
    }

    private void switchFork(BlockCapsule newHead) throws NonCommonBlockException, ReceiptCheckErrException, BadBlockException {
        Pair<LinkedList<KhaosDatabase.KhaosBlock>, LinkedList<KhaosDatabase.KhaosBlock>> binaryTree;
        try {
            binaryTree = khaosDb.getBranch(newHead.getBlockId(), getDynamicPropertiesStore().getLatestBlockHeaderHash());
        } catch (NonCommonBlockException e) {
            log.info("there is not the most recent constant ancestor, need to remove all blocks in the fork chain.");
            BlockCapsule tmp = newHead;
            while (tmp != null) {
                khaosDb.removeBlk(tmp.getBlockId());
                tmp = khaosDb.getBlock(tmp.getParentHash());
            }
            throw e;
        }

        if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
            while (!getDynamicPropertiesStore().getLatestBlockHeaderHash().equals(binaryTree.getValue().peekLast().getParentHash())) {
                eraseBlock();
            }
        }

        if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
            List<KhaosDatabase.KhaosBlock> first = new ArrayList<>(binaryTree.getKey());
            Collections.reverse(first);
            for (KhaosDatabase.KhaosBlock item : first) {
                Exception exception = null;
                try (ISession tmpSession = revokingStore.buildSession()) {
                    applyBlock(item.getBlk());
                    tmpSession.commit();
                } catch (ValidateSignatureException
                        | ContractValidateException
                        | ContractExeException
                        | TaposException
                        | DupTransactionException
                        | TransactionExpirationException
                        | ReceiptCheckErrException
                        | TooBigTransactionException
                        | ValidateScheduleException
                        | BadBlockException
                        | VMIllegalException e) {
                    log.warn(e.getMessage(), e);
                    exception = e;
                } finally {
                    if (exception != null) {
                        log.warn("switch back because exception thrown while switching forks. " + exception.getMessage(), exception);

                        first.forEach(khaosBlock -> khaosDb.removeBlk(khaosBlock.getBlk().getBlockId()));
                        khaosDb.setHead(binaryTree.getValue().peekFirst());

                        while (!getDynamicPropertiesStore().getLatestBlockHeaderHash().equals(binaryTree.getValue().peekLast().getParentHash())) {
                            eraseBlock();
                        }

                        List<KhaosDatabase.KhaosBlock> second = new ArrayList<>(binaryTree.getValue());
                        Collections.reverse(second);
                        for (KhaosDatabase.KhaosBlock khaosBlock : second) {
                            try (ISession tmpSession = revokingStore.buildSession()) {
                                applyBlock(khaosBlock.getBlk());
                                tmpSession.commit();
                            } catch (ValidateSignatureException
                                    | ContractValidateException
                                    | ContractExeException
                                    | TaposException
                                    | DupTransactionException
                                    | TransactionExpirationException
                                    | TooBigTransactionException
                                    | ValidateScheduleException
                                    | VMIllegalException e) {
                                log.warn(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * save a block
     */
    public synchronized void pushBlock(final BlockCapsule block)
            throws ValidateSignatureException, ContractValidateException,
            ContractExeException, UnLinkedBlockException, VMIllegalException,
            ValidateScheduleException, TaposException, TooBigTransactionException,
            DupTransactionException, TransactionExpirationException, BadNumberBlockException,
            BadBlockException, NonCommonBlockException, ReceiptCheckErrException, AuthorizeException {
        long start = System.currentTimeMillis();
        PendingManager pm = new PendingManager(this, block);
        try {
            if (!block.generatedByMyself) {
                if (!block.validateSignature(this.dynamicPropertiesStore, this.accountStore)) {
                    log.error("The signature is not validated.");
                    throw new BadBlockException("The signature is not validated");
                }

                if (!block.calcMerkleRoot().equals(block.getMerkleRoot())) {
                    log.error("The merkle root doesn't match, Calc result is " + block.calcMerkleRoot()
                            + " , the headers is " + block.getMerkleRoot());
                    throw new BadBlockException("The merkle hash is not validated");
                }
                consensus.receiveBlock(block);
            }
            BlockCapsule newBlock = this.khaosDb.push(block);
            // DB don't need lower block
            if (getDynamicPropertiesStore().getLatestBlockHeaderHash() == null) {
                if (newBlock.getNum() != 0) {
                    return;
                }
            } else {
                if (newBlock.getNum() <= getDynamicPropertiesStore().getLatestBlockHeaderNumber()) {
                    return;
                }
                // switch fork
                if (!newBlock.getParentHash().equals(getDynamicPropertiesStore().getLatestBlockHeaderHash())) {
                    log.warn("switch fork! new head num = {}, block id = {}", newBlock.getNum(), newBlock.getBlockId());
                    // exec switch fork
                    switchFork(newBlock);
                    return;
                }

                try (ISession tmpSession = revokingStore.buildSession()) {
                    //apply block
                    applyBlock(newBlock);
                    tmpSession.commit();
                } catch (Throwable throwable) {
                    log.error(throwable.getMessage(), throwable);
                    khaosDb.removeBlk(block.getBlockId());
                    throw throwable;
                }
            }
            log.info("save block: {}", newBlock);
        } catch (Throwable throwable) {
            log.error(throwable.getMessage(), throwable);
            khaosDb.removeBlk(block.getBlockId());
            throw throwable;
        } finally {
            pm.close();
        }
        //clear ownerAddressSet
        synchronized (pushTransactionQueue) {
            if (CollectionUtils.isNotEmpty(ownerAddressSet)) {
                Set<String> result = new HashSet<>();
                for (TransactionCapsule transactionCapsule : repushTransactions) {
                    filterOwnerAddress(transactionCapsule, result);
                }
                for (TransactionCapsule transactionCapsule : pushTransactionQueue) {
                    filterOwnerAddress(transactionCapsule, result);
                }
                ownerAddressSet.clear();
                ownerAddressSet.addAll(result);
            }
        }
        log.debug("pushBlock block number:{}, cost/txs:{}/{}",
                block.getNum(), System.currentTimeMillis() - start, block.getTransactions().size());
    }

    private void updateDynamicProperties(BlockCapsule block) {
        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(block.getBlockId().getByteString());
        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());

        revokingStore.setMaxSize((int) (dynamicPropertiesStore.getLatestBlockHeaderNumber()
                - dynamicPropertiesStore.getLatestSolidifiedBlockNum() + 1));

        khaosDb.setMaxSize((int) (dynamicPropertiesStore.getLatestBlockHeaderNumber()
                - dynamicPropertiesStore.getLatestSolidifiedBlockNum() + 1));
    }

    /**
     * Get the fork branch.
     */
    public LinkedList<BlockCapsule.BlockId> getBlockChainHashesOnFork(final BlockCapsule.BlockId forkBlockHash)
            throws NonCommonBlockException {
        final Pair<LinkedList<KhaosDatabase.KhaosBlock>, LinkedList<KhaosDatabase.KhaosBlock>> branch =
                this.khaosDb.getBranch(getDynamicPropertiesStore().getLatestBlockHeaderHash(), forkBlockHash);
        LinkedList<KhaosDatabase.KhaosBlock> blockCapsules = branch.getValue();
        if (blockCapsules.isEmpty()) {
            log.info("empty branch {}", forkBlockHash);
            return Lists.newLinkedList();
        }
        LinkedList<BlockCapsule.BlockId> result = blockCapsules.stream()
                .map(KhaosDatabase.KhaosBlock::getBlk)
                .map(BlockCapsule::getBlockId)
                .collect(Collectors.toCollection(LinkedList::new));
        result.add(blockCapsules.peekLast().getBlk().getParentBlockId());
        return result;
    }

    /**
     * judge id.
     *
     * @param blockHash blockHash
     */
    public boolean containBlock(final Sha256Hash blockHash) {
        try {
            return this.khaosDb.containBlockInMiniStore(blockHash) || blockStore.get(blockHash.getBytes()) != null;
        } catch (ItemNotFoundException | BadItemException e) {
            return false;
        }
    }

    public boolean containBlockInMainChain(BlockCapsule.BlockId blockId) {
        try {
            return blockStore.get(blockId.getBytes()) != null;
        } catch (ItemNotFoundException | BadItemException e) {
            return false;
        }
    }

    /**
     * Get a BlockCapsule by id.
     */
    public BlockCapsule getBlockById(final Sha256Hash hash) throws BadItemException, ItemNotFoundException {
        BlockCapsule block = this.khaosDb.getBlock(hash);
        if (block == null) {
            block = blockStore.get(hash.getBytes());
        }
        return block;
    }

    /**
     * judge has blocks.
     */
    private boolean hasBlocks() {
        return blockStore.iterator().hasNext() || this.khaosDb.hasData();
    }

    /**
     * Process transaction.
     */
    private Protocol.TransactionInfo processTransaction(final TransactionCapsule txCap, BlockCapsule blockCap)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            TransactionExpirationException, TooBigTransactionException, DupTransactionException,
            TaposException, ReceiptCheckErrException, VMIllegalException {

        if (txCap == null) {
            log.error("txCap is null");
            return null;
        }

        validateDup(txCap);
        validateTapos(txCap);
        validateCommon(txCap);

        TransactionTrace trace = new TransactionTrace(txCap, StoreFactory.getInstance(), new RuntimeImpl(this));
        txCap.setTxTrace(trace);

        trace.checkIsConstant();
        trace.init(blockCap);
        trace.exec();

        if (Objects.nonNull(blockCap)) {
            trace.setResult();
            if (blockCap.hasMasterSignature()) {
                trace.check();
            }
        }

        trace.finalization();
        if (Objects.nonNull(blockCap)) {
            // set the result of the transaction execution
            txCap.setResult(trace.getTransactionContext());
        }

        TransactionInfoCapsule transactionInfo = TransactionInfoCapsule.buildInstance(txCap, blockCap, trace);
        if (Objects.nonNull(blockCap) && !blockCap.getInstance().getBlockHeader().getMasterSignature().isEmpty()) {
            this.transactionHistoryStore.put(txCap.getTransactionId().getBytes(), transactionInfo);
        }

        return transactionInfo.getInstance();
    }

    /**
     * Get the block id from the number.
     */
    public BlockCapsule.BlockId getBlockIdByNum(final long num) throws ItemNotFoundException {
        return this.blockIndexStore.get(num);
    }

    public BlockCapsule getBlockByNum(final long num) throws ItemNotFoundException, BadItemException {
        return getBlockById(getBlockIdByNum(num));
    }

    /**
     * Generate a block.
     */
    public synchronized BlockCapsule generateBlock(Param.Miner miner, long blockTime, long timeout) {
        long headBlockNum = getHeadBlockNum() + 1;
        BlockCapsule blockCapsule = new BlockCapsule(headBlockNum, getHeadBlockId(), blockTime, miner.getMasterAddress());
        log.info("Generate block, current block number: " + headBlockNum);
        blockCapsule.generatedByMyself = true;
        session.reset();
        session.setValue(revokingStore.buildSession());

        Iterator<TransactionCapsule> iterator = pendingTransactions.iterator();
        while (iterator.hasNext()) {

            // check timeout
      /*if (System.currentTimeMillis() > timeout) {
        log.warn("Processing transaction time exceeds the producing time.");
        break;
      }*/

            TransactionCapsule tx = iterator.next();

            // check the block size
            long blockSize = blockCapsule.getInstance().getSerializedSize() + tx.getSerializedSize() + 3;
            if (blockSize > Parameter.ChainConstant.BLOCK_SIZE) {
                log.info("block size is {}, exceed {}", blockSize, Parameter.ChainConstant.BLOCK_SIZE);
                break;
            }

            Contract contract = tx.getInstance().getRawData().getContract();
            byte[] owner = TransactionCapsule.getOwner(contract);
            String ownerAddress = ByteArray.toHexString(owner);
            if (ownerAddressSet.contains(ownerAddress)) {
                tx.setVerified(false);
            }

            // process transaction
            try (ISession tmpSession = revokingStore.buildSession()) {
                boolean dupTrans = false;
                String txID = Hex.toHexString(tx.getTransactionId().getBytes());
                List<Protocol.Transaction> transactions = blockCapsule.getInstance().getTransactionsList();
                for (Protocol.Transaction transaction : transactions) {
                    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
                    if (txID.equals(Hex.toHexString(transactionCapsule.getTransactionId().getBytes()))) {
                        dupTrans = true;
                        break;
                    }
                }
                if (!dupTrans) {
                    processTransaction(tx, blockCapsule);
                    tmpSession.merge();
                    blockCapsule.addTransaction(tx);
                }
            } catch (Exception e) {
                log.debug("Process tx failed when generating block: {}", e.getMessage());
            } finally {
                iterator.remove();
            }
        } //end while
        session.reset();
        blockCapsule.setMerkleRoot();
        blockCapsule.sign(miner.getPrivateKey());
        log.debug("Generate block success, pendingCount: {}, repushCount: {}", pendingTransactions.size(), repushTransactions.size());
        return blockCapsule;
    }

    private void filterOwnerAddress(TransactionCapsule transactionCapsule, Set<String> result) {
        Protocol.Transaction.Contract contract = transactionCapsule.getInstance().getRawData().getContract();
        byte[] owner = TransactionCapsule.getOwner(contract);
        String ownerAddress = ByteArray.toHexString(owner);
        if (ownerAddressSet.contains(ownerAddress)) {
            result.add(ownerAddress);
        }
    }

    public TransactionStore getTransactionStore() {
        return this.transactionStore;
    }

    public TransactionHistoryStore getTransactionHistoryStore() {
        return this.transactionHistoryStore;
    }

    public BlockStore getBlockStore() {
        return this.blockStore;
    }

    /**
     * process block.
     */
    private void processBlock(BlockCapsule block) throws ValidateSignatureException, ContractValidateException,
            ContractExeException, TaposException, TooBigTransactionException, DupTransactionException, BadBlockException,
            TransactionExpirationException, ValidateScheduleException, ReceiptCheckErrException, VMIllegalException {
        // checkMaster
        if (!consensus.validBlock(block)) {
            throw new ValidateScheduleException("validateMasterSchedule error");
        }
        //parallel check sign
        if (!block.generatedByMyself) {
            try {
                preValidateTransactionSign(block);
            } catch (InterruptedException e) {
                log.error("parallel check sign interrupted exception! block info: {}", block, e);
                Thread.currentThread().interrupt();
            }
        }

        TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule(block);
        for (TransactionCapsule transactionCapsule : block.getTransactions()) {
            transactionCapsule.setBlockNum(block.getNum());
            if (block.generatedByMyself) {
                transactionCapsule.setVerified(true);
            }
            Protocol.TransactionInfo result = processTransaction(transactionCapsule, block);
            transactionStore.put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
            if (Objects.nonNull(result)) {
                transactionRetCapsule.addTransactionInfo(result);
            }
        }
        block.setResult(transactionRetCapsule);

        if (!consensus.applyBlock(block)) {
            throw new BadBlockException("consensus apply block failed");
        }

        updateTransHashCache(block);
        updateRecentBlock(block);
        updateDynamicProperties(block);
    }

    private void updateTransHashCache(BlockCapsule block) {
        for (TransactionCapsule transactionCapsule : block.getTransactions()) {
            this.transactionIdCache.put(transactionCapsule.getTransactionId(), true);
        }
    }

    private void updateRecentBlock(BlockCapsule block) {
        this.recentBlockStore.put(ByteArray.subArray(ByteArray.fromLong(block.getNum()), 6, 8),
                new BytesCapsule(ByteArray.subArray(block.getBlockId().getBytes(), 8, 16)));
    }

    private void updateFork(BlockCapsule block) {
        forkController.update(block);
    }

    public long getSyncBeginNumber() {
        log.info("headNumber:" + dynamicPropertiesStore.getLatestBlockHeaderNumber());
        log.info("syncBeginNumber:" + (dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size()));
        log.info("solidBlockNumber:" + dynamicPropertiesStore.getLatestSolidifiedBlockNum());
        return dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size();
    }

    public BlockCapsule.BlockId getSolidBlockId() {
        try {
            long num = dynamicPropertiesStore.getLatestSolidifiedBlockNum();
            return getBlockIdByNum(num);
        } catch (Exception e) {
            return getGenesisBlockId();
        }
    }

    public void closeAllStore() {
        log.info("******** begin to close db ********");
        closeOneStore(accountStore);
        closeOneStore(blockStore);
        closeOneStore(blockIndexStore);
        closeOneStore(masterStore);
        closeOneStore(masterScheduleStore);
        closeOneStore(dynamicPropertiesStore);
        closeOneStore(transactionStore);
        closeOneStore(codeStore);
        closeOneStore(peersStore);
        closeOneStore(recentBlockStore);
        closeOneStore(transactionHistoryStore);
        closeOneStore(transactionRetStore);
        log.info("******** end to close db ********");
    }

    private void closeOneStore(ILedgerYiBase database) {
        log.info("******** begin to close " + database.getName() + " ********");
        try {
            database.close();
        } catch (Exception e) {
            log.info("failed to close  " + database.getName() + ". " + e);
        } finally {
            log.info("******** end to close " + database.getName() + " ********");
        }
    }

    public boolean isTooManyPending() {
        return getPendingTransactions().size() + getRepushTransactions().size() > MAX_TRANSACTION_PENDING;
    }

    private void preValidateTransactionSign(BlockCapsule block)
            throws InterruptedException, ValidateSignatureException {
        log.debug("PreValidate Transaction Sign, size:" + block.getTransactions().size() + ",block num:" + block.getNum());
        int transSize = block.getTransactions().size();
        if (transSize <= 0) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(transSize);
        List<Future<Boolean>> futures = new ArrayList<>(transSize);

        for (TransactionCapsule transaction : block.getTransactions()) {
            Future<Boolean> future = validateSignService.submit(new ValidateSignTask(transaction, countDownLatch, this));
            futures.add(future);
        }
        countDownLatch.await();

        for (Future<Boolean> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new ValidateSignatureException(e.getCause().getMessage());
            }
        }
    }

    private void rePush(TransactionCapsule tx) {
        if (containsTransaction(tx)) {
            return;
        }
        try {
            this.pushTransaction(tx);
        } catch (ValidateSignatureException | ContractValidateException | ContractExeException | VMIllegalException e) {
            log.error(e.getMessage(), e);
        } catch (DupTransactionException e) {
            log.error("pending manager: dup trans", e);
        } catch (TaposException e) {
            log.error("pending manager: tapos exception", e);
        } catch (TooBigTransactionException e) {
            log.error("too big transaction");
        } catch (TransactionExpirationException e) {
            log.error("expiration transaction");
        } catch (ReceiptCheckErrException e) {
            log.error("outOfSlotTime transaction");
        }
    }

    private void prepareStoreFactory() {
        StoreFactory.init();
        StoreFactory.getInstance().setChainBaseManager(chainBaseManager);
    }

    private static class ValidateSignTask implements Callable<Boolean> {

        private TransactionCapsule tx;
        private CountDownLatch countDownLatch;
        private Manager manager;

        ValidateSignTask(TransactionCapsule tx, CountDownLatch countDownLatch, Manager manager) {
            this.tx = tx;
            this.countDownLatch = countDownLatch;
            this.manager = manager;
        }

        @Override
        public Boolean call() throws ValidateSignatureException {
            try {
                tx.validateSignature();
            } catch (ValidateSignatureException e) {
                throw e;
            } finally {
                countDownLatch.countDown();
            }
            return true;
        }
    }
}
