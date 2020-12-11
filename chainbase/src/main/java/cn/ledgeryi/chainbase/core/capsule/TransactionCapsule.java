package cn.ledgeryi.chainbase.core.capsule;

import cn.ledgeryi.chainbase.actuator.TransactionFactory;
import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.chainbase.core.db.TransactionTrace;
import cn.ledgeryi.common.core.exception.*;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.ReflectUtils;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.SignatureInterface;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.Protocol.Transaction.Result;
import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import cn.ledgeryi.protos.Protocol.Transaction.raw;
import com.google.protobuf.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.ledgeryi.common.core.exception.P2pException.TypeEnum.PROTOBUF_ERROR;

@Slf4j(topic = "capsule")
public class TransactionCapsule implements ProtoCapsule<Transaction> {

  private static final ExecutorService executorService = Executors.newFixedThreadPool(DBConfig.getValidContractProtoThreadNum());
  private static final String OWNER_ADDRESS = "ownerAddress_";

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;
  @Setter
  @Getter
  private long blockNum = -1;
  @Getter
  @Setter
  private TransactionTrace txTrace;
  private StringBuffer toStringBuff = new StringBuffer();
  @Getter
  @Setter
  private long time;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionCapsule(Transaction tx) {
    this.transaction = tx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionCapsule(byte[] data) throws BadItemException {
    try {
      this.transaction = Transaction.parseFrom(Message.getCodedInputStream(data));
    } catch (Exception e) {
      throw new BadItemException("Transaction proto data parse exception");
    }
  }

  public TransactionCapsule(CodedInputStream codedInputStream) throws BadItemException {
    try {
      this.transaction = Transaction.parseFrom(codedInputStream);
    } catch (IOException e) {
      throw new BadItemException("Transaction proto data parse exception");
    }
  }

  public TransactionCapsule(raw rawData, ByteString signature) {
    this.transaction = Transaction.newBuilder().setRawData(rawData).setSignature(signature).build();
  }

  public TransactionCapsule(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            (message instanceof Any ? (Any) message : Any.pack(message))).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public static byte[] getOwner(Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      Class<? extends GeneratedMessageV3> clazz = TransactionFactory.getContract(contract.getType());
      if (clazz == null) {
        log.error("not exist {}", contract.getType());
        return null;
      }
      GeneratedMessageV3 generatedMessageV3 = contractParameter.unpack(clazz);
      ByteString owner = ReflectUtils.getFieldValue(generatedMessageV3, OWNER_ADDRESS);
      if (owner == null) {
        log.error("not exist [{}] field,{}", OWNER_ADDRESS, clazz);
        return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      log.error(ex.getMessage());
      return null;
    }
  }

  public static <T extends com.google.protobuf.Message> T parse(Class<T> clazz,
      CodedInputStream codedInputStream) throws InvalidProtocolBufferException {
    T defaultInstance = Internal.getDefaultInstance(clazz);
    return (T) defaultInstance.getParserForType().parseFrom(codedInputStream);
  }

  public static void validContractProto(List<Transaction> transactionList) throws P2pException {
    List<Future<Boolean>> futureList = new ArrayList<>();
    transactionList.forEach(transaction -> {
      Future<Boolean> future = executorService.submit(() -> {
        try {
          validContractProto(transaction.getRawData().getContract());
          return true;
        } catch (Exception e) {
          log.error("{}", e.getMessage());
        }
        return false;
      });
      futureList.add(future);
    });
    for (Future<Boolean> future : futureList) {
      try {
        if (!future.get()) {
          throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
        }
      } catch (Exception e) {
        throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
      }
    }
  }

  public static void validContractProto(Transaction.Contract contract) throws InvalidProtocolBufferException, P2pException {
    Any contractParameter = contract.getParameter();
    Class clazz = TransactionFactory.getContract(contract.getType());
    if (clazz == null) {
      throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
    }
    com.google.protobuf.Message src = contractParameter.unpack(clazz);
    com.google.protobuf.Message contractMessage = parse(clazz, Message.getCodedInputStream(src.toByteArray()));

    Message.compareBytes(src.toByteArray(), contractMessage.toByteArray());
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    SignatureInterface signature = SignUtils.fromComponents(r, s, v, DBConfig.isECKeyCryptoEngine());
    return signature.toBase64();
  }

  public void setResult(TransactionResultCapsule transactionResultCapsule) {
    this.transaction = this.getInstance().toBuilder().addRet(transactionResultCapsule.getInstance())
        .build();
  }

  public void setReference(long blockNum, byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
        .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getExpiration() {
    return transaction.getRawData().getExpiration();
  }

  public void setExpiration(long expiration) {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration).build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public void setTimestamp() {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getTimestamp() {
    return transaction.getRawData().getTimestamp();
  }

  @Deprecated
  public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(Any.pack(message)).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getMerkleHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(DBConfig.isECKeyCryptoEngine(), transBytes);
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(DBConfig.isECKeyCryptoEngine(),
        this.transaction.getRawData().toByteArray());
  }

  public void sign(byte[] privateKey) {
    SignInterface cryptoEngine = SignUtils.fromPrivate(privateKey, DBConfig.isECKeyCryptoEngine());
    ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine.signHash(getRawHash().getBytes())));
    this.transaction = this.transaction.toBuilder().setSignature(sig).build();
  }

  /**
   * validate tx's signature
   */
  public boolean validateSignature() throws ValidateSignatureException {
    if (isVerified) {
      return true;
    }

    Transaction.Contract contract = this.transaction.getRawData().getContract();
    try {
      byte[] owner = getOwner(contract);
      byte[] address = SignUtils.signatureToAddress(getRawHash().getBytes(),
              getBase64FromByteString(this.transaction.getSignature()), DBConfig.isECKeyCryptoEngine());
      if (!Arrays.equals(owner, address)) {
        isVerified = false;
        throw new ValidateSignatureException("sig error");
      }
    } catch (SignatureException e) {
      isVerified = false;
      throw new ValidateSignatureException(e.getMessage());
    }

    isVerified = true;
    return true;
  }

  public Sha256Hash getTransactionId() {
    return getRawHash();
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  public long getSerializedSize() {
    return this.transaction.getSerializedSize();
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  @Override
  public String toString() {

    toStringBuff.setLength(0);
    toStringBuff.append("TransactionCapsule \n[ ");

    toStringBuff.append("hash=").append(getTransactionId()).append("\n");
    AtomicInteger i = new AtomicInteger();
    if (getInstance().getRawData().getContract() != null) {
      toStringBuff.append("contract list:{ ");
      Transaction.Contract contract = getInstance().getRawData().getContract();
      toStringBuff.append("[" + i + "] ").append("type: ").append(contract.getType()).append("\n");
      toStringBuff.append("from address=").append(getOwner(contract)).append("\n");
      toStringBuff.append("to address=").append("").append("\n");
      toStringBuff.append("sign=").append(getBase64FromByteString(this.transaction.getSignature())).append("\n");
      toStringBuff.append("}\n");
    } else {
      toStringBuff.append("contract list is empty\n");
    }
    toStringBuff.append("]");
    return toStringBuff.toString();
  }

  public void setResult(TransactionContext context) {
    this.setResultCode(context.getProgramResult().getResultCode());
  }

  public void setResultCode(ContractResult code) {
    Result ret = Result.newBuilder().setContractRet(code).build();
    if (this.transaction.getRetCount() > 0) {
      ret = this.transaction.getRet(0).toBuilder().setContractRet(code).build();
      this.transaction = transaction.toBuilder().setRet(0, ret).build();
      return;
    }
    this.transaction = transaction.toBuilder().addRet(ret).build();
  }

  public ContractResult getContractRet() {
    if (this.transaction.getRetCount() <= 0) {
      return null;
    }
    return this.transaction.getRet(0).getContractRet();
  }
}
