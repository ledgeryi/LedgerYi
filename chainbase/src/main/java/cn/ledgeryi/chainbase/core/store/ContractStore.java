package cn.ledgeryi.chainbase.core.store;

import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

@Slf4j(topic = "DB")
@Component
public class ContractStore extends LedgerYiStoreWithRevoking<ContractCapsule> {

    @Autowired
    private ContractStore(@Value("contract") String dbName) {
        super(dbName);
    }

    @Override
    public ContractCapsule get(byte[] key) {
        return getUnchecked(key);
    }

    /**
     * find a transaction by it's id.
     */
    public byte[] findContractByHash(byte[] txHash) {
        return revokingDB.getUnchecked(txHash);
    }

    public ABI getABI(byte[] contractAddress) {
        byte[] value = revokingDB.getUnchecked(contractAddress);
        if (ArrayUtils.isEmpty(value)) {
            return null;
        }
        ContractCapsule contractCapsule = new ContractCapsule(value);
        SmartContractOuterClass.SmartContract smartContract = contractCapsule.getInstance();
        if (smartContract == null) {
            return null;
        }
        return smartContract.getAbi();
    }

}
