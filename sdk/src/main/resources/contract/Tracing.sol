// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;

pragma solidity ^0.6.9;

import "./DataSet.sol";
import "./StringSet.sol";
import "./AddressSet.sol";

contract Tracing {

    using StringSet for StringSet.Set;
    using DataSet for DataSet.DataList;
    using AddressSet for AddressSet.WhiteList;

    string private uid;

    address private owner;

    string private groupName;

    uint private createTime;

    StringSet.Set private keys;

    AddressSet.WhiteList private contractWhiteList;

    mapping(bytes32 => DataSet.DataList) private dataList;

    mapping(bytes32 => mapping(bytes32 => AddressSet.WhiteList)) private dataWhiteList;

    modifier onlyContractOwner() {
        require(msg.sender == contractWhiteList.owner, "Caller is not contract owner");
        _;
    }

    modifier onlyContractWhiteListMember() {
        require(msg.sender == contractWhiteList.owner || !contractWhiteList.status || contractWhiteList.contains(msg.sender), "Caller is not in whiteList");
        _;
    }

    modifier onlyDataOwner(string memory _traceUid, uint256 _index) {
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        require(msg.sender == dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].owner, "Caller is not data owner");
        _;
    }

    constructor (string memory _uid, string memory _groupName) public {
        uid = _uid;
        groupName = _groupName;
        createTime = block.timestamp;
        owner = msg.sender;
        contractWhiteList.owner = msg.sender;
        contractWhiteList.status = true;
    }


    function getUid() external view returns(string memory) {
        return uid;
    }

    function getOwner() external view returns (address) {
        return owner;
    }

    function getCreateTime() external view returns (uint) {
        return createTime;
    }

    function getBaseInfo() external view returns(string memory, string memory, uint, address) {
        return (groupName, uid, createTime, owner);
    }

    function disableContractWhite() external onlyContractOwner {
        contractWhiteList.status = false;
    }

    function enableContractWhite() external onlyContractOwner {
        contractWhiteList.status = true;
    }

    function getStatusOfContractWhite() external onlyContractOwner view returns (bool) {
        return contractWhiteList.status;
    }

    function addUsersToContractWhiteList(address[] memory _users) external onlyContractOwner returns (bool) {
        for (uint256 i = 0; i < _users.length; i++) {
            require(!contractWhiteList.contains(_users[i]), "The user already exist");
            addUserToContractWhiteList(_users[i]);
        }
        return true;
    }

    function addUserToContractWhiteList(address _user) public onlyContractOwner returns (bool) {
        require(contractWhiteList.status, "The status of white list is disable");
        return contractWhiteList.addUser(_user);
    }

    function removeUsersFromContractWhiteList(address[] memory _users) external onlyContractOwner returns (bool) {
        for (uint256 i = 0; i < _users.length; i++) {
            if(contractWhiteList.contains(_users[i])){
                contractWhiteList.remove(_users[i]);
            }
        }
        return true;
    }

    function removeUserFromContractWhiteList(address _user) external onlyContractOwner returns (bool) {
        require(contractWhiteList.contains(_user), "The user does not exist");
        return contractWhiteList.remove(_user);
    }

    function getUserSizeOfContractWhiteList() external view returns (uint256) {
        return contractWhiteList.length();
    }

    function getUsersFromContractWhiteList(uint256 _startIndex, uint256 _size) external view returns (address[] memory) {
        require(_startIndex < contractWhiteList.length(), "Start index out of bounds");
        uint256 _length = 0;
        if (_startIndex + _size <= contractWhiteList.length()) {
            _length = _size;
        } else {
            _length = contractWhiteList.length() - _startIndex;
        }
        address[] memory users = new address[](_length);
        for (uint256 i = 0; i < _length; i++) {
            users[i] = contractWhiteList.at(_startIndex + i);
        }
        return users;
    }

    function getUserFromContractWhiteList(uint256 _index) external view returns (address) {
        require(_index < contractWhiteList.length(), "User index out of bounds");
        return contractWhiteList.at(_index);
    }

    function addDataKey(string[] memory _dataKeys) external onlyContractWhiteListMember {
        for (uint256 i = 0; i < _dataKeys.length; i++) {
            keys.add(_dataKeys[i]);
        }
    }

    function getDataKey() external view returns (string[] memory) {
        return keys._values;
    }

    function saveDataInfo(string memory _traceUid, string[] memory _dataInfos) external onlyContractWhiteListMember returns (uint256) {
        require(keys.length() != 0, "Store infos is empty");
        uint256 _key = dataList[keccak256(abi.encodePacked(_traceUid))].addTraceData(keys._values, _dataInfos);
        dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_key))].setOwner(msg.sender);
        dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_key))].addUser(msg.sender);
        dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_key))].status = true;
        return _key;
    }

    function dataVerify(string memory _traceUid, uint256 _index,string[] memory _dataInfos) external view returns (bool) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        return dataList[keccak256(abi.encodePacked(_traceUid))].traceDataVerify( _index, keys._values, _dataInfos);
    }

    function getDataInfo(string memory _traceUid) external view returns (string[] memory, string[] memory) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        uint256 _index = dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length - 1;
        require(!dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].status || dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].contains(msg.sender), "Caller is not in whiteList");
        return dataList[keccak256(abi.encodePacked(_traceUid))].getTraceData(keys._values,_index);
    }

    function getDataInfo(string memory _traceUid, uint256 _index) external view returns (string[] memory, string[] memory) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        require(!dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].status || dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].contains(msg.sender), "Caller is not in whiteList");
        return dataList[keccak256(abi.encodePacked(_traceUid))].getTraceData(keys._values,_index);
    }

    function getDataInfoLength(string memory _traceUid) external view returns (uint256) {
        return dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length;
    }

    function addUsersToDataWhiteList(string memory _traceUid, uint256 _index, address[] memory _users) external onlyDataOwner(_traceUid,_index) returns (bool) {
        for (uint256 i = 0; i < _users.length; i++) {
            require(!dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].contains(_users[i]), "The user already exist");
            addUserToDataWhiteList(_traceUid, _index, _users[i]);
        }
        return true;
    }

    function addUserToDataWhiteList(string memory _traceUid, uint256 _index, address _user) public onlyDataOwner(_traceUid,_index) returns (bool) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].addUser(_user);
    }

    function removeUsersFromDataWhiteList(string memory _traceUid, uint256 _index, address[] memory _users) external onlyDataOwner(_traceUid,_index) returns (bool) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        for (uint256 i = 0; i < _users.length; i++) {
            if(dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].contains(_users[i])){
                dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].remove(_users[i]);
            }
        }
        return true;
    }

    function removeUserFromDataWhiteList(string memory _traceUid, uint256 _index, address _user) external onlyDataOwner(_traceUid,_index) returns (bool) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        require(dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].contains(_user), "The user does not exist");
        return dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].remove(_user);
    }

    function getUserSizeOfDataWhiteList(string memory _traceUid, uint256 _index) external view returns (uint256) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].length();
    }

    function getUsersFromDataWhiteList(string memory _traceUid, uint256 _index, uint256 _startIndex, uint256 _size) external view returns (address[] memory) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        uint256 _whiteListLength = dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].length();
        require(_startIndex < _whiteListLength, "Start index out of bounds");
        uint256 _length = 0;
        if (_startIndex + _size <= _whiteListLength) {
            _length = _size;
        } else {
            _length = _whiteListLength - _startIndex;
        }
        address[] memory users = new address[](_length);
        for (uint256 i = 0; i < _length; i++) {
            users[i] = dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].at(_startIndex + i);
        }
        return users;
    }

    function getUserFromDataWhiteList(string memory _traceUid, uint256 _index, uint256 _userIndex) external view returns (address) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].at(_userIndex);
    }

    function disableDataWhite(string memory _traceUid, uint256 _index) external onlyContractOwner {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].status = false;
    }

    function enableDataWhite(string memory _traceUid, uint256 _index) external onlyContractOwner {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].status =  true;
    }

    function getStatusOfDataWhite(string memory _traceUid, uint256 _index) external onlyContractOwner view returns (bool) {
        require(0 != dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Trace id invalid");
        require(_index < dataList[keccak256(abi.encodePacked(_traceUid))]._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_traceUid))][keccak256(abi.encodePacked(_index))].status;
    }
}