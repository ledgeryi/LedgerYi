package cn.ledgeryi.framework.core.services.http;

import java.util.HashMap;
import java.util.Map;

public class HttpSelfFormatFieldName {

  private static Map<String, Integer> AddressFieldNameMap = new HashMap<>();
  private static Map<String, Integer> NameFieldNameMap = new HashMap<>();

  static {
    //***** api.proto *****
    //PrivateParameters
    AddressFieldNameMap.put("protocol.PrivateParameters.transparent_from_address", 1);
    AddressFieldNameMap.put("protocol.PrivateParameters.transparent_to_address", 1);

    //***** LedgerYi.proto *****
    //AccountId
    AddressFieldNameMap.put("protocol.AccountId.address", 1);
    //Vote
    AddressFieldNameMap.put("protocol.Vote.vote_address", 1);
    //Account
    AddressFieldNameMap.put("protocol.Account.address", 1);
    //Key
    AddressFieldNameMap.put("protocol.Key.address", 1);
    //Master
    AddressFieldNameMap.put("protocol.Master.address", 1);
    //Votes
    AddressFieldNameMap.put("protocol.Votes.address", 1);
    //TransactionInfo
    AddressFieldNameMap.put("protocol.TransactionInfo.Log.address", 1);
    AddressFieldNameMap.put("protocol.TransactionInfo.contract_address", 1);
    //BlockHeader
    AddressFieldNameMap.put("protocol.BlockHeader.raw.master_address", 1);
    //SmartContract
    AddressFieldNameMap.put("protocol.SmartContract.origin_address", 1);
    AddressFieldNameMap.put("protocol.SmartContract.contract_address", 1);

    //***** api.proto *****
    //Return
    NameFieldNameMap.put("protocol.Return.message", 1);
    //Address
    NameFieldNameMap.put("protocol.Address.host", 1);
    //Note
    NameFieldNameMap.put("protocol.Note.memo", 1);

    //***** Contract.proto *****
    //TransferAssetContract
    NameFieldNameMap.put("protocol.TransferAssetContract.asset_name", 1);


    //***** LedgerYi.proto *****
    //AccountId
    NameFieldNameMap.put("protocol.AccountId.name", 1);
    //Account
    NameFieldNameMap.put("protocol.Account.account_name", 1);
    NameFieldNameMap.put("protocol.Account.asset_issued_name", 1);
    NameFieldNameMap.put("protocol.Account.asset_issued_ID", 1);
    NameFieldNameMap.put("protocol.Account.account_id", 1);
    //authority
    NameFieldNameMap.put("protocol.authority.permission_name", 1);
    //Transaction
    NameFieldNameMap.put("protocol.Transaction.Contract.ContractName", 1);
    //TransactionInfo
    NameFieldNameMap.put("protocol.TransactionInfo.resMessage", 1);
  }

  public static boolean isAddressFormat(final String name) {
    return AddressFieldNameMap.containsKey(name);
  }

  public static boolean isNameStringFormat(final String name) {
    return NameFieldNameMap.containsKey(name);
  }
}
