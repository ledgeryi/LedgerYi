package cn.ledgeryi.framework.common.net.udp.handler;

import java.net.InetSocketAddress;
import cn.ledgeryi.framework.common.net.udp.message.Message;

public class UdpEvent {

  private Message message;
  private InetSocketAddress address;

  public UdpEvent(Message message, InetSocketAddress address) {
    this.message = message;
    this.address = address;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public void setAddress(InetSocketAddress address) {
    this.address = address;
  }
}
