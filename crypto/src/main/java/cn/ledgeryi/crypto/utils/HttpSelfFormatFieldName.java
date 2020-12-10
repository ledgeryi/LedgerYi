package cn.ledgeryi.crypto.utils;

import java.util.HashMap;
import java.util.Map;

public class HttpSelfFormatFieldName {

  private static Map<String, Integer> AddressFieldNameMap = new HashMap<>();
  private static Map<String, Integer> NameFieldNameMap = new HashMap<>();

  static {
    //***** Contract.proto *****
    //TransferContract
    AddressFieldNameMap.put("protocol.TransferContract.owner_address", 1);
    AddressFieldNameMap.put("protocol.TransferContract.to_address", 1);

    //***** ledger_yi.proto *****
    //AccountId
    AddressFieldNameMap.put("protocol.AccountId.address", 1);
    //Vote
    AddressFieldNameMap.put("protocol.Vote.vote_address", 1);
    //Proposal
    AddressFieldNameMap.put("protocol.Proposal.proposer_address", 1);
    AddressFieldNameMap.put("protocol.Proposal.approvals", 1);
    //Exchange
    AddressFieldNameMap.put("protocol.Exchange.creator_address", 1);
    //Account
    AddressFieldNameMap.put("protocol.Account.address", 1);
    //Key
    AddressFieldNameMap.put("protocol.Key.address", 1);
    //DelegatedResource
    AddressFieldNameMap.put("protocol.DelegatedResource.from", 1);
    AddressFieldNameMap.put("protocol.DelegatedResource.to", 1);
    //Master
    AddressFieldNameMap.put("protocol.Master.address", 1);
    //Votes
    AddressFieldNameMap.put("protocol.Votes.address", 1);
    //TransactionInfo
    AddressFieldNameMap.put("protocol.TransactionInfo.Log.address", 1);
    AddressFieldNameMap.put("protocol.TransactionInfo.contract_address", 1);
    //BlockHeader
    AddressFieldNameMap.put("protocol.BlockHeader.raw.master_address", 1);

    //***** api.proto *****
    //Return
    NameFieldNameMap.put("protocol.Return.message", 1);
    //Address
    NameFieldNameMap.put("protocol.Address.host", 1);
    //Note
    NameFieldNameMap.put("protocol.Note.memo", 1);

    //***** ledger_yi.proto *****
    //AccountId
    NameFieldNameMap.put("protocol.AccountId.name", 1);
    //Exchange
    NameFieldNameMap.put("protocol.Exchange.first_token_id", 1);
    NameFieldNameMap.put("protocol.Exchange.second_token_id", 1);
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
