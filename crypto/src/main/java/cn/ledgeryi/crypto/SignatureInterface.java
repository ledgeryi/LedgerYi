package cn.ledgeryi.crypto;

public interface SignatureInterface {
  boolean validateComponents();

  byte[] toByteArray();

  String toBase64();
}