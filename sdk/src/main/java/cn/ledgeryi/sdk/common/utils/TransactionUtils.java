package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignatureInterface;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.*;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.config.Configuration;
import com.google.protobuf.ByteString;

import java.security.SignatureException;
import java.util.Arrays;

public class TransactionUtils {


  public static byte[] getOwner(Protocol.Transaction.Contract contract) {
    ByteString owner;
    try {
      switch (contract.getType()) {
        case TransferContract:
          owner = contract.getParameter().unpack(BalanceContract.TransferContract.class).getOwnerAddress();
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
      v += 27; // revId -> v
    }
    SignatureInterface signature = SignUtils.fromComponents(r, s, v, Configuration.isEckey());
    //ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }

  public static boolean validTransaction(Protocol.Transaction signedTransaction) {
    Protocol.Transaction.Contract contract = signedTransaction.getRawData().getContract();
    byte[] hash = Sha256Sm3Hash.hash(signedTransaction.getRawData().toByteArray());
    try {
      byte[] owner = getOwner(contract);
      byte[] address = ECKey.signatureToAddress( hash, getBase64FromByteString(signedTransaction.getSignature()));
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
}
