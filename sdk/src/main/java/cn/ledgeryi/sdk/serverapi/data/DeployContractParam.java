package cn.ledgeryi.sdk.serverapi.data;

import cn.ledgeryi.sdk.common.utils.AbiUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

@Builder
public class DeployContractParam {
    @NonNull
    @Getter
    private String abi;
    @NonNull
    @Getter
    private String contractName;
    @NonNull
    @Getter
    @Setter
    private String contractByteCodes;

    @Setter
    private String constructor;

    @Setter
    private List<Object> args;

    public String getContractByteCodes() {
        if (StringUtils.isNotEmpty(constructor) && CollectionUtils.isNotEmpty(args)){
            return contractByteCodes += Hex.toHexString(AbiUtil.parseConstructor(constructor, args));
        }
        return contractByteCodes;
    }
}
