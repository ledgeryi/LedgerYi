package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.SignatureInterface;
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

  public static Protocol.Transaction sign(Protocol.Transaction transaction, byte[] privKeyBytes) {
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Sm3Hash.hash(transaction.getRawData().toByteArray());
    SignInterface ecKeyEngine = SignUtils.fromPrivate(privKeyBytes, Configuration.isEckey());
    ByteString sig = ByteString.copyFrom(ecKeyEngine.Base64toBytes(ecKeyEngine.signHash(hash)));
    transactionBuilderSigned.setSignature(sig);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

}
