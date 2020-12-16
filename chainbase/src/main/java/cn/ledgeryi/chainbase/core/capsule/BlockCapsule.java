package cn.ledgeryi.chainbase.core.capsule;

import cn.ledgeryi.chainbase.common.utils.AdjustBalanceUtil;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.utils.MerkleTree;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.common.utils.DecodeUtil;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.common.utils.Time;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.core.exception.ValidateSignatureException;
import cn.ledgeryi.protos.Protocol.Block;
import cn.ledgeryi.protos.Protocol.BlockHeader;
import cn.ledgeryi.protos.Protocol.Transaction;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "capsule")
public class BlockCapsule implements ProtoCapsule<Block> {

  public boolean generatedByMyself = false;
  @Getter
  @Setter
  private TransactionRetCapsule result;
  private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);

  private Block block;
  private List<TransactionCapsule> transactions = new ArrayList<>();
  private StringBuffer toStringBuff = new StringBuffer();

  public BlockCapsule(long number, Sha256Hash hash, long when, ByteString masterAddress) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number)
        .setParentHash(hash.getByteString())
        .setTimestamp(when)
        .setVersion(Parameter.ChainConstant.BLOCK_VERSION)
        .setMasterAddress(masterAddress)
        .build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

    // block
    Block.Builder blockBuild = Block.newBuilder();
    this.block = blockBuild.setBlockHeader(blockHeader).build();
    initTxs();
  }


  public BlockCapsule(long timestamp, ByteString parentHash, long number, List<Transaction> transactionList) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setTimestamp(timestamp)
        .setParentHash(parentHash)
        .setNumber(number)
        .build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

    // block
    Block.Builder blockBuild = Block.newBuilder();
    transactionList.forEach(tx -> blockBuild.addTransactions(tx));
    this.block = blockBuild.setBlockHeader(blockHeader).build();
    initTxs();
  }

  public BlockCapsule(Block block) {
    this.block = block;
    initTxs();
  }

  public BlockCapsule(byte[] data) throws BadItemException {
    try {
      this.block = Block.parseFrom(data);
      initTxs();
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("Block proto data parse exception");
    }
  }

  public BlockCapsule(CodedInputStream codedInputStream) throws BadItemException {
    try {
      this.block = Block.parseFrom(codedInputStream);
      initTxs();
    } catch (Exception e) {
      log.error("constructor block error : {}", e.getMessage());
      throw new BadItemException("Block proto data parse exception");
    }
  }

  public void addTransaction(TransactionCapsule pendingTx) {
    this.block = this.block.toBuilder().addTransactions(pendingTx.getInstance()).build();
    getTransactions().add(pendingTx);
  }

  public List<TransactionCapsule> getTransactions() {
    return transactions;
  }

  private void initTxs() {
    transactions = this.block.getTransactionsList().stream().map(TransactionCapsule::new).collect(Collectors.toList());
  }

  public void sign(byte[] privateKey) {
    if (ArrayUtils.isEmpty(privateKey)) {
      log.error("sign error, private key is null");
      return;
    }
    SignInterface ecKeyEngine = SignUtils.fromPrivate(privateKey, DBConfig.isECKeyCryptoEngine());
    ByteString sig = ByteString.copyFrom(ecKeyEngine.Base64toBytes(ecKeyEngine.signHash(getRawHash().getBytes())));
    BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setMasterSignature(sig).build();
    this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(DBConfig.isECKeyCryptoEngine(), this.block.getBlockHeader().getRawData().toByteArray());
  }

  public boolean validateSignature(DynamicPropertiesStore dynamicPropertiesStore, AccountStore accountStore)
          throws ValidateSignatureException {
    try {
      byte[] sigAddress = SignUtils.signatureToAddress(getRawHash().getBytes(),
              TransactionCapsule.getBase64FromByteString(block.getBlockHeader().getMasterSignature()),
              DBConfig.isECKeyCryptoEngine());
      byte[] masterAccountAddress = block.getBlockHeader().getRawData().getMasterAddress().toByteArray();
      return Arrays.equals(sigAddress, masterAccountAddress);
    } catch (SignatureException e) {
      throw new ValidateSignatureException(e.getMessage());
    }
  }

  public BlockId getBlockId() {
    if (blockId.equals(Sha256Hash.ZERO_HASH)) {
      blockId = new BlockId(Sha256Hash.of(DBConfig.isECKeyCryptoEngine(), this.block.getBlockHeader().getRawData().toByteArray()),
          getNum());
    }
    return blockId;
  }

  public Sha256Hash calcMerkleRoot() {
    List<Transaction> transactionsList = this.block.getTransactionsList();

    if (CollectionUtils.isEmpty(transactionsList)) {
      return Sha256Hash.ZERO_HASH;
    }

    Vector<Sha256Hash> ids = transactionsList.stream()
        .map(TransactionCapsule::new)
        .map(TransactionCapsule::getMerkleHash)
        .collect(Collectors.toCollection(Vector::new));

    return MerkleTree.getInstance().createTree(ids).getRoot().getHash();
  }

  public void setMerkleRoot() {
    BlockHeader.raw blockHeaderRaw =
        this.block.getBlockHeader().getRawData().toBuilder()
            .setTxTrieRoot(calcMerkleRoot().getByteString()).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  public void setAccountStateRoot(byte[] root) {
    BlockHeader.raw blockHeaderRaw =
        this.block.getBlockHeader().getRawData().toBuilder()
            .setAccountStateRoot(ByteString.copyFrom(root)).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  /* only for genisis */
  public void setMaster(String master) {
    BlockHeader.raw blockHeaderRaw =
        this.block.getBlockHeader().getRawData().toBuilder().setMasterAddress(
            ByteString.copyFrom(master.getBytes())).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getTxTrieRoot());
  }

  public Sha256Hash getAccountRoot() {
    if (this.block.getBlockHeader().getRawData().getAccountStateRoot() != null
        && !this.block.getBlockHeader().getRawData().getAccountStateRoot().isEmpty()) {
      return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getAccountStateRoot());
    }
    return Sha256Hash.ZERO_HASH;
  }

  public ByteString getMasterAddress() {
    return this.block.getBlockHeader().getRawData().getMasterAddress();
  }

  @Override
  public byte[] getData() {
    return this.block.toByteArray();
  }

  @Override
  public Block getInstance() {
    return this.block;
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getParentHash());
  }

  public BlockId getParentBlockId() {
    return new BlockId(getParentHash(), getNum() - 1);
  }

  public long getNum() {
    return this.block.getBlockHeader().getRawData().getNumber();
  }

  public long getTimeStamp() {
    return this.block.getBlockHeader().getRawData().getTimestamp();
  }

  public boolean hasMasterSignature() {
    return !getInstance().getBlockHeader().getMasterSignature().isEmpty();
  }

  @Override
  public String toString() {
    toStringBuff.setLength(0);
    toStringBuff.append("BlockCapsule \n[ ");
    toStringBuff.append("hash=").append(getBlockId()).append("\n");
    toStringBuff.append("number=").append(getNum()).append("\n");
    toStringBuff.append("parentId=").append(getParentHash()).append("\n");
    toStringBuff.append("master address=").append(DecodeUtil.createReadableString(getMasterAddress().toByteArray())).append("\n");
    toStringBuff.append("generated by myself=").append(generatedByMyself).append("\n");
    toStringBuff.append("generate time=").append(Time.getTimeString(getTimeStamp())).append("\n");
    toStringBuff.append("account root=").append(getAccountRoot()).append("\n");
    if (!getTransactions().isEmpty()) {
      toStringBuff.append("merkle root=").append(getMerkleRoot()).append("\n");
      toStringBuff.append("txs size=").append(getTransactions().size()).append("\n");
    } else {
      toStringBuff.append("txs are empty\n");
    }
    toStringBuff.append("]");
    return toStringBuff.toString();
  }

  public static class BlockId extends Sha256Hash {

    private long num;

    public BlockId() {
      super(Sha256Hash.ZERO_HASH.getBytes());
      num = 0;
    }

    public BlockId(Sha256Hash blockId) {
      super(blockId.getBytes());
      byte[] blockNum = new byte[8];
      System.arraycopy(blockId.getBytes(), 0, blockNum, 0, 8);
      num = Longs.fromByteArray(blockNum);
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockId(Sha256Hash hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(byte[] hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(ByteString hash, long num) {
      super(num, hash.toByteArray());
      this.num = num;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Hash))) {
        return false;
      }
      return Arrays.equals(getBytes(), ((Sha256Hash) o).getBytes());
    }

    public String getString() {
      return "Num:" + num + ",ID:" + super.toString();
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public int compareTo(Sha256Hash other) {
      if (other.getClass().equals(BlockId.class)) {
        long otherNum = ((BlockId) other).getNum();
        return Long.compare(num, otherNum);
      }
      return super.compareTo(other);
    }

    public long getNum() {
      return num;
    }
  }
}
