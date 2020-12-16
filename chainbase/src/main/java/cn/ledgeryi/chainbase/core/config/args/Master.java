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

  @Getter
  private String url;

  @Getter
  @Setter
  private long voteCount;

  public void setAddress(final byte[] address) {
    if (!DecodeUtil.addressValid(address)) {
      throw new IllegalArgumentException(
              "The address(" + DecodeUtil.createReadableString(address) + ") must be a 21 bytes.");
    }
    this.address = address;
  }

  public void setUrl(final String url) {
    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException("The url(" + url + ") format error.");
    }
    this.url = url;
  }
}
