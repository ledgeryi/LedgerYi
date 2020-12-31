package cn.ledgeryi.contract.vm.config;

import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.chainbase.core.store.StoreFactory;

public class ConfigLoader {

    public static void load(StoreFactory storeFactory){

        DynamicPropertiesStore ds = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
        if (ds != null) {
            //todo vm's config info
        }

    }
}
