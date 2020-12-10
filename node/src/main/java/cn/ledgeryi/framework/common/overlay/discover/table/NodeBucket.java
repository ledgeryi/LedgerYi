package cn.ledgeryi.framework.common.overlay.discover.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kest on 5/25/15.
 */
public class NodeBucket {

  private final int depth;
  //每个桶中能够存储的节点最大条目，默认参数为KademliaOptions.BUCKET_SIZE
  //节点以NodeEntry形式存在NodeBucket中
  private final List<NodeEntry> nodes = new ArrayList<>();

  NodeBucket(int depth) {
    this.depth = depth;
  }

  public int getDepth() {
    return depth;
  }

  public synchronized NodeEntry addNode(NodeEntry e) {
    if (!nodes.contains(e)) {
      if (nodes.size() >= KademliaOptions.BUCKET_SIZE) {
        return getLastSeen();
      } else {
        nodes.add(e);
      }
    }

    return null;
  }

  private NodeEntry getLastSeen() {
    List<NodeEntry> sorted = nodes;
    Collections.sort(sorted, new TimeComparator());
    return sorted.get(0);
  }

  public synchronized void dropNode(NodeEntry entry) {
    for (NodeEntry e : nodes) {
      if (e.getId().equals(entry.getId())) {
        nodes.remove(e);
        break;
      }
    }
  }

  public int getNodesCount() {
    return nodes.size();
  }

  public List<NodeEntry> getNodes() {
    return nodes;
  }
}
