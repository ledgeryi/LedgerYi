package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.SignatureInterface;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.BalanceContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.config.Configuration;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.security.SignatureException;
import java.util.Arrays;

@Slf4j
public class TransactionUtils {

  public static byte[] getOwner(Protocol.Transaction.Contract contract) {
    ByteString owner;
    try {
      switch (contract.getType()) {
        case CreateSmartContract:
          owner = contract.getParameter().unpack(SmartContractOuterClass.CreateSmartContract.class).getOwnerAddress();
          break;
        case TriggerSmartContract:
          owner = contract.getParameter().unpack(SmartContractOuterClass.TriggerSmartContract.class).getOwnerAddress();
          break;
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27;
    }
    SignatureInterface signature = SignUtils.fromComponents(r, s, v, Configuration.isEckey());
    return signature.toBase64();
  }

  public static boolean validTransaction(Protocol.Transaction signedTransaction) {
    Protocol.Transaction.Contract contract = signedTransaction.getRawData().getContract();
    byte[] hash = Sha256Sm3Hash.hash(signedTransaction.getRawData().toByteArray());
    try {
      byte[] owner = getOwner(contract);
      byte[] address = SignUtils.signatureToAddress(hash, getBase64FromByteString(signedTransaction.getSignature()), Configuration.isEckey());
      if (!Arrays.equals(owner, address)) {
        return false;
      }
    } catch (SignatureException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public static Protocol.Transaction sign(Protocol.Transaction transaction, byte[] privKeyBytes) throws SignatureException, InvalidProtocolBufferException {
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Sm3Hash.hash(transaction.getRawData().toByteArray());
    SignInterface ecKeyEngine = SignUtils.fromPrivate(privKeyBytes, Configuration.isEckey());
    ByteString sig = ByteString.copyFrom(ecKeyEngine.Base64toBytes(ecKeyEngine.signHash(hash)));

    /*byte[] address = SignUtils.signatureToAddress(hash, getBase64FromByteString(sig), Configuration.isEckey());
    ByteString owner = transaction.getRawData().getContract().getParameter().unpack(SmartContractOuterClass.CreateSmartContract.class).getOwnerAddress();
    if (!Arrays.equals(address, owner.toByteArray())) {
      System.out.println("address decode error!");
    }*/
    transactionBuilderSigned.setSignature(sig);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static Protocol.Transaction setTimestamp(Protocol.Transaction transaction) {
    long currentTime = System.currentTimeMillis(); // 1000000 + System.nanoTime()%1000000;
    Protocol.Transaction.Builder builder = transaction.toBuilder();
    Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData().toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    return builder.build();
  }

  public static Protocol.Transaction setExpirationTime(Protocol.Transaction transaction) {
    if (transaction.getSignature() == null) {
      long expirationTime = System.currentTimeMillis() + 6 * 60 * 60 * 1000;
      Protocol.Transaction.Builder builder = transaction.toBuilder();
      Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData().toBuilder();
      rowBuilder.setExpiration(expirationTime);
      builder.setRawData(rowBuilder.build());
      return builder.build();
    }
    return transaction;
  }

  public void showTransactionAfterSign(Protocol.Transaction transaction) throws InvalidProtocolBufferException {
    log.info("after sign transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    log.info("txid is " + ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray())));
    if (transaction.getRawData().getContract().getType() == Protocol.Transaction.Contract.ContractType.CreateSmartContract) {
      SmartContractOuterClass.CreateSmartContract createSmartContract = transaction.getRawData().getContract().getParameter().unpack(SmartContractOuterClass.CreateSmartContract.class);
      byte[] contractAddress = generateContractAddress(createSmartContract.getOwnerAddress().toByteArray(), transaction);
      log.info("Your smart contract address will be: " + DecodeUtil.createReadableString(contractAddress));
    }
  }

  private byte[] generateContractAddress(byte[] ownerAddress, Protocol.Transaction trx) {
    byte[] txRawDataHash = Sha256Sm3Hash.of(trx.getRawData().toByteArray()).getBytes();
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);
    return Hash.sha3omit12(combined);
  }

}
