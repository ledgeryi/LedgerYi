package cn.ledgeryi.chainbase.core.capsule;

import java.util.Arrays;

import cn.ledgeryi.chainbase.common.utils.DBConfig;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.common.utils.Sha256Hash;

@Slf4j(topic = "capsule")
public class CodeCapsule implements ProtoCapsule<byte[]> {

  private byte[] code;

  public CodeCapsule(byte[] code) {
    this.code = code;
  }

  public Sha256Hash getCodeHash() {
    return Sha256Hash.of(DBConfig.isECKeyCryptoEngine(), this.code);
  }

  @Override
  public byte[] getData() {
    return this.code;
  }

  @Override
  public byte[] getInstance() {
    return this.code;
  }

  @Override
  public String toString() {
    return Arrays.toString(this.code);
  }
}
