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

  public VotesCapsule getVotes() {
    if (ArrayUtils.isEmpty(any)) {
      return null;
    }
    return new VotesCapsule(any);
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

  public static void main(String[] args) {
    String value = "0a14ada95a8734256b797efcd862e0b208529283ac561214f51fa1d6bf8e93f0f8ccd4d7a607483e35fc09ae1a380a191a0872657472696576652a091a0775696e74323536300240020a1b1a0573746f7265220e12036e756d1a0775696e743235363002400322e601608060405234801561001057600080fd5b5060c78061001f6000396000f3fe6080604052348015600f57600080fd5b506004361060325760003560e01c80632e64cec11460375780636057361d146053575b600080fd5b603d607e565b6040518082815260200191505060405180910390f35b607c60048036036020811015606757600080fd5b81019080803590602001909291905050506087565b005b60008054905090565b806000819055505056fea2646970667358221220e0d62b2700e3afba6e87729d482239a5322dc2bdc290f9b75029586f2e2b115864736f6c63430007040033304b3a0753746f7261676540d086034a203ea7a4f00800d122c515df282ba155adeacb0e2fcb3be8e075abc631e65bb5b8";
    Value value1 = new Value(Hex.decode(value), Type.VALUE_TYPE_CREATE);
    ContractCapsule contract = value1.getContract();
    System.out.println(contract.getInstance().getAbi().toString());
  }
}
