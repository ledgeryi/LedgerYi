package cn.ledgeryi.framework.common.overlay.discover;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;

@Slf4j(topic = "discover")
public class RefreshTask extends DiscoverTask {

  public RefreshTask(NodeManager nodeManager) {
    super(nodeManager);
  }

  @Override
  public void run() {
    discover(Node.getNodeId(), 0, new ArrayList<>());
  }
}
