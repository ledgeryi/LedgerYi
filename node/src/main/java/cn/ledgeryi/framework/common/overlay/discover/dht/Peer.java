package cn.ledgeryi.framework.common.overlay.discover.dht;

import java.math.BigInteger;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;
import cn.ledgeryi.framework.common.utils.Utils;

public class Peer {

  byte[] id;
  String host = "127.0.0.1";
  int port = 0;

  public Peer(byte[] id, String host, int port) {
    this.id = id;
    this.host = host;
    this.port = port;
  }

  public Peer(byte[] ip) {
    this.id = ip;
  }

  public static byte[] randomPeerId() {

    byte[] peerIdBytes = new BigInteger(512, Utils.getRandom()).toByteArray();

    final String peerId;
    if (peerIdBytes.length > 64) {
      peerId = Hex.toHexString(peerIdBytes, 1, 64);
    } else {
      peerId = Hex.toHexString(peerIdBytes);
    }

    return Hex.decode(peerId);
  }

  public byte nextBit(String startPattern) {

    if (this.toBinaryString().startsWith(startPattern + "1")) {
      return 1;
    } else {
      return 0;
    }
  }

  public byte[] calcDistance(Peer toPeer) {

    BigInteger aaPeer = new BigInteger(getId());
    BigInteger bbPeer = new BigInteger(toPeer.getId());

    BigInteger distance = aaPeer.xor(bbPeer);
    return BigIntegers.asUnsignedByteArray(distance);
  }

  public byte[] getId() {
    return id;
  }

  public void setId(byte[] id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return String
        .format("Peer {\n id=%s, \n host=%s, \n port=%d\n}", Hex.toHexString(id), host, port);
  }

  public String toBinaryString() {

    BigInteger bi = new BigInteger(1, id);
    String out = String.format("%512s", bi.toString(2));
    out = out.replace(' ', '0');

    return out;
  }

}
