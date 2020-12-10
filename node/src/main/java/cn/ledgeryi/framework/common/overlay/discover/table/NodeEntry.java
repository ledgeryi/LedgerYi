package cn.ledgeryi.framework.common.overlay.discover.table;

import cn.ledgeryi.framework.common.overlay.discover.node.Node;

/**
 * Created by kest on 5/25/15.
 */
public class NodeEntry {

  //该节点的ID
  private byte[] ownerId;
  //该节点自身
  private Node node;
  //节点IP
  private String entryId;
  //该节点到本地节点的距离
  private int distance;
  //最后一次与该节点交互的时间
  private long modified;

  public NodeEntry(Node n) {
    this.node = n;
    this.ownerId = n.getId();
    entryId = n.getHost();
    distance = distance(ownerId, n.getId());
    touch();
  }

  public NodeEntry(byte[] ownerId, Node n) {
    this.node = n;
    this.ownerId = ownerId;
    entryId = n.getHost();
    distance = distance(ownerId, n.getId());
    touch();
  }

  public static int distance(byte[] ownerId, byte[] targetId) {
    byte[] h1 = targetId;
    byte[] h2 = ownerId;

    byte[] hash = new byte[Math.min(h1.length, h2.length)];

    for (int i = 0; i < hash.length; i++) {
      hash[i] = (byte) (((int) h1[i]) ^ ((int) h2[i]));
    }

    int d = KademliaOptions.BINS;

    for (byte b : hash) {
      if (b == 0) {
        d -= 8;
      } else {
        int count = 0;
        for (int i = 7; i >= 0; i--) {
          boolean a = ((b & 0xff) & (1 << i)) == 0;
          if (a) {
            count++;
          } else {
            break;
          }
        }

        d -= count;

        break;
      }
    }
    return d;
  }

  public void touch() {
    modified = System.currentTimeMillis();
  }

  public int getDistance() {
    return distance;
  }

  public String getId() {
    return entryId;
  }

  public Node getNode() {
    return node;
  }

  public long getModified() {
    return modified;
  }

  @Override
  public boolean equals(Object o) {
    boolean ret = false;

    if (o != null && this.getClass() == o.getClass()) {
      NodeEntry e = (NodeEntry) o;
      ret = this.getId().equals(e.getId());
    }

    return ret;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }
}
