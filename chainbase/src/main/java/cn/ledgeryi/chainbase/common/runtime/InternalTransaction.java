package cn.ledgeryi.chainbase.common.runtime;

import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol.Transaction;
import com.google.common.primitives.Longs;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cn.ledgeryi.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class InternalTransaction {

  private Transaction transaction;
  private byte[] hash;
  private byte[] parentHash;
  /* the amount of tx to transfer (calculated as sun) */
  private long value;

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  private byte[] receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  private byte[] data;
  private long nonce;
  private byte[] transferToAddress;

  /*  Message sender address */
  private byte[] sendAddress;
  @Getter
  private int deep;
  @Getter
  private int index;
  private boolean rejected;
  private String note;
  private byte[] protoEncoded;

  /**
   * Construct a child InternalTransaction
   */
  public InternalTransaction(byte[] parentHash, int deep, int index, byte[] sendAddress, byte[] transferToAddress,
                             long value, byte[] data, String note, long nonce) {
    this.parentHash = parentHash.clone();
    this.deep = deep;
    this.index = index;
    this.note = note;
    this.sendAddress = ArrayUtils.nullToEmpty(sendAddress);
    this.transferToAddress = ArrayUtils.nullToEmpty(transferToAddress);
    if ("create".equalsIgnoreCase(note)) {
      this.receiveAddress = EMPTY_BYTE_ARRAY;
    } else {
      this.receiveAddress = ArrayUtils.nullToEmpty(transferToAddress);
    }
    this.value = value;
    this.data = ArrayUtils.nullToEmpty(data);
    this.nonce = nonce;
    this.hash = getHash();
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public byte[] getTransferToAddress() {
    return transferToAddress.clone();
  }

  public void reject() {
    this.rejected = true;
  }

  public boolean isRejected() {
    return rejected;
  }

  public String getNote() {
    if (note == null) {
      return "";
    }
    return note;
  }

  public byte[] getSender() {
    if (sendAddress == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return sendAddress.clone();
  }

  public long getValue() {
    return value;
  }

  public byte[] getData() {
    if (data == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return data.clone();
  }

  public final byte[] getHash() {
    if (!isEmpty(hash)) {
      return Arrays.copyOf(hash, hash.length);
    }

    byte[] plainMsg = this.getEncoded();
    byte[] nonceByte;
    nonceByte = Longs.toByteArray(nonce);
    byte[] forHash = new byte[plainMsg.length + nonceByte.length];
    System.arraycopy(plainMsg, 0, forHash, 0, plainMsg.length);
    System.arraycopy(nonceByte, 0, forHash, plainMsg.length, nonceByte.length);
    this.hash = Hash.sha3(forHash);
    return Arrays.copyOf(hash, hash.length);
  }

  public long getNonce() {
    return nonce;
  }

  public byte[] getEncoded() {
    if (protoEncoded != null) {
      return protoEncoded.clone();
    }
    byte[] parentHashArray = parentHash.clone();

    if (parentHashArray == null) {
      parentHashArray = EMPTY_BYTE_ARRAY;
    }
    byte[] valueByte = Longs.toByteArray(this.value);
    byte[] raw = new byte[parentHashArray.length + this.receiveAddress.length + this.data.length
        + valueByte.length];
    System.arraycopy(parentHashArray, 0, raw, 0, parentHashArray.length);
    System
        .arraycopy(this.receiveAddress, 0, raw, parentHashArray.length, this.receiveAddress.length);
    System.arraycopy(this.data, 0, raw, parentHashArray.length + this.receiveAddress.length,
        this.data.length);
    System.arraycopy(valueByte, 0, raw,
        parentHashArray.length + this.receiveAddress.length + this.data.length,
        valueByte.length);
    this.protoEncoded = raw;
    return protoEncoded.clone();
  }

  public enum TxType {
    TX_PRECOMPILED_TYPE,
    TX_CONTRACT_CREATION_TYPE,
    TX_CONTRACT_CALL_TYPE,
    TX_UNKNOWN_TYPE,
  }

}
