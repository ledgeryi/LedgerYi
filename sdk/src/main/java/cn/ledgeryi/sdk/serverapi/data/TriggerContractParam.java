package cn.ledgeryi.sdk.serverapi.data;

import cn.ledgeryi.sdk.common.utils.AbiUtil;
import lombok.Getter;
import lombok.NonNull;
import org.spongycastle.util.encoders.Hex;

public class TriggerContractParam {

    @NonNull
    private String triggerMethod;

    @NonNull
    private String args;

    @NonNull
    @Getter
    private long callValue;

    @NonNull
    @Getter
    private boolean isConstant;

    @NonNull
    @Getter
    private byte[] contractAddress;

    private byte[] data;

    public byte[] getData() {
        return Hex.decode(AbiUtil.parseMethod(triggerMethod, args, false));
    }

    public TriggerContractParam setTriggerMethod(String triggerMethod) {
        this.triggerMethod = triggerMethod;
        return this;
    }

    public TriggerContractParam setArgs(String args) {
        this.args = args;
        return this;
    }

    public TriggerContractParam setCallValue(long callValue) {
        this.callValue = callValue;
        return this;
    }

    public TriggerContractParam setConstant(boolean constant) {
        isConstant = constant;
        return this;
    }

    public TriggerContractParam setContractAddress(byte[] contractAddress) {
        this.contractAddress = contractAddress;
        return this;
    }
}
