package cn.ledgeryi.framework.core.capsule.utils;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Protocol;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import cn.ledgeryi.framework.core.config.args.Args;

public class BlockUtil {

  /**
   * create genesis block from transactions.
   */
  public static BlockCapsule newGenesisBlockCapsule() {
    Args args = Args.getInstance();
    GenesisBlock genesisBlockArg = args.getGenesisBlock();
    List<Protocol.Transaction> transactionList = genesisBlockArg.getAssets().stream().map(key -> {
              byte[] address = key.getAddress();
              long balance = key.getBalance();
              return TransactionUtil.newGenesisTransaction(address, balance);}).collect(Collectors.toList());
    long timestamp = Long.parseLong(genesisBlockArg.getTimestamp());
    ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString(genesisBlockArg.getParentHash()));
    long number = Long.parseLong(genesisBlockArg.getNumber());
    BlockCapsule blockCapsule = new BlockCapsule(timestamp, parentHash, number, transactionList);
    blockCapsule.setMerkleRoot();
    blockCapsule.setMaster("A new system must allow existing systems to be linked together without "
            + "requiring any central control or coordination");
    blockCapsule.generatedByMyself = true;
    return blockCapsule;
  }

  /**
   * Whether the hash of the judge block is equal to the hash of the parent block.
   */
  public static boolean isParentOf(BlockCapsule blockCapsule1, BlockCapsule blockCapsule2) {
    return blockCapsule1.getBlockId().equals(blockCapsule2.getParentHash());
  }
}
