package cn.ledgeryi.chainbase.actuator;

import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public interface Actuator {

  boolean execute(Object result) throws ContractExeException;

  boolean validate() throws ContractValidateException;

  ByteString getOwnerAddress() throws InvalidProtocolBufferException;
}
