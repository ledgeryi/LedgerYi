// SPDX-License-Identifier: GPL-3.0

pragma solidity ^0.6.9;

contract Storage {

    struct Data{
        address storeData;
        uint32 dataId;
        bool used;
    }

    mapping(bytes32 => Data) private dataStore;

    function store(uint32 _dataId, address _data) public {
        dataStore[keccak256(abi.encode(_dataId))] = Data(_data,_dataId,true);
    }

    function retrieve(uint32 _dataId) public view returns (address, uint32, bool){
        Data storage tmp = dataStore[keccak256(abi.encode(_dataId))];
        return (tmp.storeData,tmp.dataId,tmp.used);
    }

    function checkUsed(uint32 _dataId) public view returns (bool) {
        Data storage tmp = dataStore[keccak256(abi.encode(_dataId))];
        return tmp.used;
    }
}
