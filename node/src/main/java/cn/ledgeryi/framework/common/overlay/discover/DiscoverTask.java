package cn.ledgeryi.framework.common.overlay.discover;

import java.util.ArrayList;
import java.util.List;

import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.discover.table.KademliaOptions;

@Slf4j(topic = "discover")
public class DiscoverTask implements Runnable {

  private NodeManager nodeManager;

  private byte[] nodeId;

  public DiscoverTask(NodeManager nodeManager) {
    this.nodeManager = nodeManager;
    this.nodeId = nodeManager.getPublicHomeNode().getId();
  }

  @Override
  public void run() {
    discover(nodeId, 0, new ArrayList<>());
  }

  public synchronized void discover(byte[] nodeId, int round, List<Node> prevTried) {

    try {
      if (round == KademliaOptions.MAX_STEPS) {
        return;
      }

      List<Node> closest = nodeManager.getTable().getClosestNodes(nodeId);
      List<Node> tried = new ArrayList<>();
      for (Node n : closest) {
        if (!tried.contains(n) && !prevTried.contains(n)) {
          try {
            //send ping
            NodeHandler nodeHandler = nodeManager.getNodeHandler(n);
            //send findNode
            nodeHandler.sendFindNode(nodeId);
            tried.add(n);
            wait(50);
          } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
          }
        }
        if (tried.size() == KademliaOptions.ALPHA) {
          break;
        }
      }

      if (tried.isEmpty()) {
        return;
      }

      tried.addAll(prevTried);

      discover(nodeId, round + 1, tried);
    } catch (Exception ex) {
      log.error("{}", ex);
    }
  }

}
