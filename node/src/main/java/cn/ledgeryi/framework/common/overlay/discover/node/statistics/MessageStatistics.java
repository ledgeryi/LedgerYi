package cn.ledgeryi.framework.common.overlay.discover.node.statistics;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.framework.common.net.udp.message.UdpMessageTypeEnum;
import cn.ledgeryi.framework.core.net.message.FetchInvDataMessage;
import cn.ledgeryi.framework.core.net.message.InventoryMessage;
import cn.ledgeryi.framework.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp ledgerYi
  public final MessageCount ledgerYiInMessage = new MessageCount();
  public final MessageCount ledgerYiMessage = new MessageCount();

  public final MessageCount ledgerYiInSyncBlockChain = new MessageCount();
  public final MessageCount ledgerYiOutSyncBlockChain = new MessageCount();
  public final MessageCount ledgerYiInBlockChainInventory = new MessageCount();
  public final MessageCount ledgerYiOutBlockChainInventory = new MessageCount();

  public final MessageCount ledgerYiInTxInventory = new MessageCount();
  public final MessageCount ledgerYiOutTxInventory = new MessageCount();
  public final MessageCount ledgerYiInTxInventoryElement = new MessageCount();
  public final MessageCount ledgerYiOutTxInventoryElement = new MessageCount();

  public final MessageCount ledgerYiInBlockInventory = new MessageCount();
  public final MessageCount ledgerYiOutBlockInventory = new MessageCount();
  public final MessageCount ledgerYiInBlockInventoryElement = new MessageCount();
  public final MessageCount ledgerYiOutBlockInventoryElement = new MessageCount();

  public final MessageCount ledgerYiInTxFetchInvData = new MessageCount();
  public final MessageCount ledgerYiOutTxFetchInvData = new MessageCount();
  public final MessageCount ledgerYiInTxFetchInvDataElement = new MessageCount();
  public final MessageCount ledgerYiOutTxFetchInvDataElement = new MessageCount();

  public final MessageCount ledgerYiInBlockFetchInvData = new MessageCount();
  public final MessageCount ledgerYiOutBlockFetchInvData = new MessageCount();
  public final MessageCount ledgerYiInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount ledgerYiOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount ledgerYiInTx = new MessageCount();
  public final MessageCount ledgerYiOutTx = new MessageCount();
  public final MessageCount ledgerYiInTxs = new MessageCount();
  public final MessageCount ledgerYiOutTxs = new MessageCount();
  public final MessageCount ledgerYiInBlock = new MessageCount();
  public final MessageCount ledgerYiOutBlock = new MessageCount();
  public final MessageCount ledgerYiOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      ledgerYiInMessage.add();
    } else {
      ledgerYiMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          ledgerYiInSyncBlockChain.add();
        } else {
          ledgerYiOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          ledgerYiInBlockChainInventory.add();
        } else {
          ledgerYiOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        if (flag) {
          if (inventoryMessage.getInvMessageType() == MessageTypes.TX) {
            ledgerYiInTxInventory.add();
            ledgerYiInTxInventoryElement.add(inventorySize);
          } else {
            ledgerYiInBlockInventory.add();
            ledgerYiInBlockInventoryElement.add(inventorySize);
          }
        } else {
          if (inventoryMessage.getInvMessageType() == MessageTypes.TX) {
            ledgerYiOutTxInventory.add();
            ledgerYiOutTxInventoryElement.add(inventorySize);
          } else {
            ledgerYiOutBlockInventory.add();
            ledgerYiOutBlockInventoryElement.add(inventorySize);
          }
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        if (flag) {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TX) {
            ledgerYiInTxFetchInvData.add();
            ledgerYiInTxFetchInvDataElement.add(fetchSize);
          } else {
            ledgerYiInBlockFetchInvData.add();
            ledgerYiInBlockFetchInvDataElement.add(fetchSize);
          }
        } else {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TX) {
            ledgerYiOutTxFetchInvData.add();
            ledgerYiOutTxFetchInvDataElement.add(fetchSize);
          } else {
            ledgerYiOutBlockFetchInvData.add();
            ledgerYiOutBlockFetchInvDataElement.add(fetchSize);
          }
        }
        break;
      case TXS:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          ledgerYiInTxs.add();
          ledgerYiInTx.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          ledgerYiOutTxs.add();
          ledgerYiOutTx.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case TX:
        if (flag) {
          ledgerYiInMessage.add();
        } else {
          ledgerYiMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          ledgerYiInBlock.add();
        }
        ledgerYiOutBlock.add();
        break;
      default:
        break;
    }
  }

}
