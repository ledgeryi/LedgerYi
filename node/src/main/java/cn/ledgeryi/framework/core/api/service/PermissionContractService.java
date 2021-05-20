package cn.ledgeryi.framework.core.api.service;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.framework.common.utils.AbiUtil;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.framework.core.permission.PermissionService;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.beust.jcommander.internal.Lists;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * it is client for permission's contract call
 */
@Slf4j
@Service
public class PermissionContractService {

    @Lazy
    @Autowired
    private LedgerYi ledgerYi;

    @Lazy
    @Autowired
    private PermissionService permissionService;

    /**
     * check whether the node exists in node list
     * @param host
     * @param port
     */
    public boolean beExistsInAccessedNodes(String host, int port) {
        try {
            List args = Lists.newArrayList(host, port);
            byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("beExistsDuplicateHostAndPort(string,uint16)", args));

            ProgramResult paramResult = callConstantContact(permissionService.getNodeMgrAddress(), methodDecode);
            return ByteUtil.byteArrayToInt(ByteString.copyFrom(paramResult.getHReturn()).toByteArray()) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private ProgramResult callConstantContact(String contractAddress, byte[] methodDecode) {
        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = SmartContractOuterClass.TriggerSmartContract.newBuilder()
                .setContractAddress(ByteString.copyFrom(DecodeUtil.decode(contractAddress)))
                .setData(ByteString.copyFrom(methodDecode))
                .setOwnerAddress(ByteString.copyFrom(DecodeUtil.decode(permissionService.getGuardianAccount())))
                .build();

        TransactionCapsule txCap;
        ProgramResult programResult;
        try {
            txCap = ledgerYi.createTransactionCapsule(triggerSmartContract, Protocol.Transaction.Contract.ContractType.TriggerSmartContract);
            programResult = ledgerYi.localCallConstantContract(txCap);
        } catch (Exception e) {
            log.error("call contract [{}] fail:{}", contractAddress, e.getMessage());
            return null;
        }

        return programResult;
    }
}
