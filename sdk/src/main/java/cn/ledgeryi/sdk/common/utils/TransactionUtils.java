package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.SignatureInterface;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.config.Configuration;
import com.google.protobuf.ByteString;
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
        case ClearABIContract:
          owner = contract.getParameter().unpack(SmartContractOuterClass.ClearABIContract.class).getOwnerAddress();
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
    SignatureInterface signature = SignUtils.fromComponents(r, s, v, Configuration.isEcc());
    return signature.toBase64();
  }

  public static boolean validTransaction(Protocol.Transaction signedTransaction) {
    Protocol.Transaction.Contract contract = signedTransaction.getRawData().getContract();
    byte[] hash = Sha256Sm3Hash.hash(signedTransaction.getRawData().toByteArray());
    try {
      byte[] owner = getOwner(contract);
      byte[] address = SignUtils.signatureToAddress(hash, getBase64FromByteString(signedTransaction.getSignature()), Configuration.isEcc());
      if (!Arrays.equals(owner, address)) {
        return false;
      }
    } catch (SignatureException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public static Protocol.Transaction sign(Protocol.Transaction transaction, byte[] privKeyBytes) {
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Sm3Hash.hash(transaction.getRawData().toByteArray());
    SignInterface ecKeyEngine = SignUtils.fromPrivate(privKeyBytes, Configuration.isEcc());
    ByteString sig = ByteString.copyFrom(ecKeyEngine.Base64toBytes(ecKeyEngine.signHash(hash)));
    transactionBuilderSigned.setSignature(sig);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static Protocol.Transaction sign(Protocol.Transaction transaction, SignInterface myKey) {
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Sm3Hash.hash(transaction.getRawData().toByteArray());
    SignatureInterface signature = myKey.sign(hash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.setSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static byte[] generateContractAddress(Protocol.Transaction tx, byte[] ownerAddress) {
    byte[] txRawDataHash = Sha256Sm3Hash.of(tx.getRawData().toByteArray()).getBytes();
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);
    return Hash.sha3omit12(combined);
  }

}
