package cn.ledgeryi.framework.core.capsule.utils;

import cn.ledgeryi.chainbase.common.utils.Commons;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.BalanceContract;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "capsule")
public class TransactionUtil {

  public static Protocol.Transaction newGenesisTransaction(byte[] key, long value)
      throws IllegalArgumentException {

    if (!Commons.addressValid(key)) {
      throw new IllegalArgumentException("Invalid address");
    }
    BalanceContract.TransferContract transferContract = BalanceContract.TransferContract.newBuilder()
        .setAmount(value)
        .setOwnerAddress(ByteString.copyFrom("0x000000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(key))
        .build();

    return new TransactionCapsule(transferContract, Protocol.Transaction.Contract.ContractType.TransferContract).getInstance();
  }
}
