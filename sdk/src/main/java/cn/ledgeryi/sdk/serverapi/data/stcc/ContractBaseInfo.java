package cn.ledgeryi.sdk.serverapi.data.stcc;

import lombok.Builder;
import lombok.ToString;

/**
 * @author Brian
 * @date 2021/8/2 14:41
 */
@Builder
@ToString
public class ContractBaseInfo {
    //合约名称
    String nameEn;
    //合约名称
    String nameZn;
    //合约地址
    String address;
    //合约创建者
    String creator;
    //合约创建者地址
    String owner;
    //合约创建时间
    String createTime;
    //溯源唯一标识
    String uid;
}
