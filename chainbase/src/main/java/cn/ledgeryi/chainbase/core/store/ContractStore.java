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
     * get total transaction.
     */
    public long getTotalContracts() {
        return Streams.stream(revokingDB.iterator()).count();
    }

    /* for test
    public void listContract(){
        Iterator<Map.Entry<byte[], byte[]>> iterator = revokingDB.iterator();
        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> next = iterator.next();
            System.out.println("=============key: " + Hex.toHexString(next.getKey()));
            System.out.println("=============value: " + Hex.toHexString(next.getValue()));
        }
    }*/


    /**
     * find a transaction  by it's id.
     */
    public byte[] findContractByHash(byte[] txHash) {
        return revokingDB.getUnchecked(txHash);
    }

    /**
     *
     * @param contractAddress
     * @return
     */
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
