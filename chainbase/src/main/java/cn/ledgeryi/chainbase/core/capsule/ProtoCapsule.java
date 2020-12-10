package cn.ledgeryi.chainbase.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
