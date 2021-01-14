package cn.ledgeryi.contract.vm.repository;

import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.common.core.exception.BadItemException;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

public class Value {

  private Type type;
  private byte[] any = null;

  public Value(byte[] any, Type type) {
    if (any != null && any.length > 0) {
      this.any = new byte[any.length];
      System.arraycopy(any, 0, this.any, 0, any.length);
      this.type = type.clone();
    }
  }

  public Value(byte[] any, int type) {
    if (any != null && any.length > 0) {
      this.any = new byte[any.length];
      System.arraycopy(any, 0, this.any, 0, any.length);
      this.type = new Type(type);
    }
  }

  private Value(Value value) {
    if (value.getAny() != null && value.getAny().length > 0) {
      this.any = new byte[value.any.length];
      System.arraycopy(value.getAny(), 0, this.any, 0, value.getAny().length);
      this.type = value.getType().clone();
    }
  }

  public static Value create(byte[] any, int type) {
    return new Value(any, type);
  }

  public static Value create(byte[] any) {
    return new Value(any, Type.VALUE_TYPE_NORMAL);
  }

  public Value clone() {
    return new Value(this);
  }

  public byte[] getAny() {
    return any;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void addType(Type type) {
    this.type.addType(type);
  }

  public void addType(int type) {
    this.type.addType(type);
  }

  public AccountCapsule getAccount() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new AccountCapsule(any);
  }

  public BytesCapsule getBytes() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new BytesCapsule(any);
  }

  public TransactionCapsule getTransaction() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    try {
      return new TransactionCapsule(any);
    } catch (BadItemException e) {
      return null;
    }
  }

  public BlockCapsule getBlock() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    try {
      return new BlockCapsule(any);
    } catch (Exception e) {
      return null;
    }
  }

  public MasterCapsule getMaster() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new MasterCapsule(any);

  }

  public BytesCapsule getBlockIndex() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new BytesCapsule(any);
  }

  public CodeCapsule getCode() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new CodeCapsule(any);
  }

  public ContractCapsule getContract() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new ContractCapsule(any);
  }

  public BytesCapsule getDynamicProperties() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new BytesCapsule(any);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    Value V = (Value) obj;
    if (Arrays.equals(this.any, V.getAny())) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return new Integer(type.hashCode() + Arrays.hashCode(any)).hashCode();
  }

}
