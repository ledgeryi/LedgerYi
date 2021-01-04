/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class Utils {
    public static final String PERMISSION_ID = "Permission_id";
    public static final String TRANSACTION = "transaction";
    public static final String VALUE = "value";

    public static String printSmartContract(SmartContractOuterClass.SmartContract smartContract){
        String smartStr = JsonFormat.printToString(smartContract, true);
        JSONObject smartJsonObject = JSONObject.parseObject(smartStr);
        JsonFormatUtil.formatJson(smartJsonObject.toJSONString());
        return JsonFormatUtil.formatJson(smartJsonObject.toJSONString());
    }

    public static String printTransactionExceptId(Protocol.Transaction transaction) {
        JSONObject jsonObject = printTransactionToJSON(transaction, true);
        jsonObject.remove("txID");
        return JsonFormatUtil.formatJson(jsonObject.toJSONString());
    }

    public static byte[] generateContractAddress(Protocol.Transaction trx, byte[] ownerAddress) {
        // get tx hash
        byte[] txRawDataHash = Sha256Sm3Hash.of(trx.getRawData().toByteArray()).getBytes();

        // combine
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);
    }

    public static JSONObject printTransactionToJSON(Protocol.Transaction transaction, boolean selfType) {
        JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction, selfType));
        JSONArray contracts = new JSONArray();
        JSONObject contractJson = null;
        Protocol.Transaction.Contract contract = transaction.getRawData().getContract();
        Any contractParameter = contract.getParameter();
        try {
            switch (contract.getType()) {
                case CreateSmartContract:
                    SmartContractOuterClass.CreateSmartContract deployContract =
                            contractParameter.unpack(SmartContractOuterClass.CreateSmartContract.class);
                    contractJson =
                            JSONObject.parseObject(JsonFormat.printToString(deployContract, selfType));
                    byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
                    byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
                    jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
                    break;
                case TriggerSmartContract:
                    SmartContractOuterClass.TriggerSmartContract triggerSmartContract =
                            contractParameter.unpack(SmartContractOuterClass.TriggerSmartContract.class);
                    contractJson =
                            JSONObject.parseObject(
                                    JsonFormat.printToString(triggerSmartContract, selfType));
                    break;

                case ClearABIContract:
                    SmartContractOuterClass.ClearABIContract clearABIContract =
                            contractParameter.unpack(SmartContractOuterClass.ClearABIContract.class);
                    contractJson =
                            JSONObject.parseObject(
                                    JsonFormat.printToString(clearABIContract, selfType));
                    break;
                // todo add other contract
                default:
            }
            JSONObject parameter = new JSONObject();
            parameter.put(VALUE, contractJson);
            parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
            JSONObject jsonContract = new JSONObject();
            jsonContract.put("parameter", parameter);
            jsonContract.put("type", contract.getType());
            if (contract.getPermissionId() > 0) {
                jsonContract.put(PERMISSION_ID, contract.getPermissionId());
            }
            contracts.add(jsonContract);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            // System.out.println("InvalidProtocolBufferException: {}", e.getMessage());
        }

        JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
        rawData.put("contract", contracts);
        jsonTransaction.put("raw_data", rawData);
        String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
        jsonTransaction.put("raw_data_hex", rawDataHex);
        String txID = ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray()));
        jsonTransaction.put("txID", txID);
        return jsonTransaction;
    }

    public static boolean isNumericString(String str) {
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHexString(String str) {
        boolean bRet = false;
        try {
            ByteArray.fromHexString(str);
            bRet = true;
        } catch (Exception e) {
        }
        return bRet;
    }
}
