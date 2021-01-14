package cn.ledgeryi.chainbase.core.config.args;

import cn.ledgeryi.common.utils.DecodeUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class Master implements Serializable {

  private static final long serialVersionUID = -7446501098542377380L;

  @Getter
  private byte[] address;

  public void setAddress(final byte[] address) {
    if (!DecodeUtil.addressValid(address)) {
      throw new IllegalArgumentException("The address(" + DecodeUtil.createReadableString(address) + ") must be a 20 bytes.");
    }
    this.address = address;
  }
}
