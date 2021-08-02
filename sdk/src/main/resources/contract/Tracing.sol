// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;

pragma solidity ^0.6.9;

import "./AddressSet.sol";
import "./DataSet.sol";

contract Tracing {

    using DataSet for DataSet.DataList;
    using AddressSet for AddressSet.WhiteList;

    string private uid;

    address private owner;

    string private creater;

    string private groupName;

    uint private createTime;

    DataSet.DataList private dataList;

    AddressSet.WhiteList private contractWhiteList;

    mapping(bytes32 => AddressSet.WhiteList) private dataWhiteList;

    modifier onlyContractOwner() {
        require(msg.sender == contractWhiteList.owner, "Caller is not contract owner");
        _;
    }

    modifier onlyContractWhiteListMember() {
        require(msg.sender == contractWhiteList.owner || !contractWhiteList.status || contractWhiteList.contains(msg.sender), "Caller is not in whiteList");
        _;
    }

    modifier onlyDataOwner(uint256 _index) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        AddressSet.WhiteList storage whiteList = dataWhiteList[keccak256(abi.encodePacked(_index))];
        require(msg.sender == whiteList.owner, "Caller is not data owner");
        _;
    }

    modifier onlyDataWhiteListMember(uint256 _index) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        AddressSet.WhiteList storage whiteList = dataWhiteList[keccak256(abi.encodePacked(_index))];
        require(!whiteList.status || whiteList.contains(msg.sender), "Caller is not in whiteList");
        _;
    }

    constructor (string memory _creater, string memory _uid, string memory _groupName) public {
        creater = _creater;
        uid = _uid;
        groupName = _groupName;
        createTime = now;
        owner = msg.sender;
        contractWhiteList.status = true;
    }

    function getCreater() external view returns (string memory) {
        return creater;
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

    function getBaseInfo() external view returns(string memory, string memory, string memory, uint, address) {
        return (creater, groupName, uid, createTime, owner);
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

    function addUserToContractWhiteList(address _user) external onlyContractOwner returns (bool) {
        require(contractWhiteList.status, "The status of white list is disabel");
        return contractWhiteList.addUser(_user);
    }

    function removeUserFromContractWhiteList(address _user) external onlyContractOwner returns (bool) {
        require(contractWhiteList.contains(_user), "The user does not exist");
        return contractWhiteList.remove(_user);
    }

    function getUserSizeOfContractWhiteList() external view returns (uint256) {
        return contractWhiteList.length();
    }

    function getUserFromContractWhiteList(uint256 _index) external view returns (address) {
        require(_index < contractWhiteList.length(), "User index out of bounds");
        return contractWhiteList.at(_index);
    }

    function addDataKey(string[] memory _dataKeys) external onlyContractWhiteListMember {
        for (uint256 i = 0; i < _dataKeys.length; i++) {
            dataList.addKey(_dataKeys[i]);
        }
    }

    function getDataKey() external view returns (string[] memory) {
        return dataList.getKey();
    }

    function saveDataInfo(string[] memory _dataInfos) external onlyContractWhiteListMember returns (uint256) {
        uint256 _key = dataList.addData(_dataInfos);
        dataWhiteList[keccak256(abi.encodePacked(_key))].setOwner(msg.sender);
        dataWhiteList[keccak256(abi.encodePacked(_key))].addUser(msg.sender);
        return _key;
    }

    function getDataInfo() external view returns (string[] memory, string[] memory) {
        uint256 _index = dataList.length() - 1;
        require(_index < dataList._datas.length, "Data index out of bounds");
        AddressSet.WhiteList storage whiteList = dataWhiteList[keccak256(abi.encodePacked(_index))];
        require(!whiteList.status || whiteList.contains(msg.sender), "Caller is not in whiteList");
        return dataList.getData(_index);
    }

    function getDataInfo(uint256 _index) external view onlyDataWhiteListMember(_index) returns (string[] memory, string[] memory) {
        return dataList.getData(_index);
    }

    function addUserToDataWhiteList(uint256 _index, address _user) external onlyDataOwner(_index) returns (bool) {
        return dataWhiteList[keccak256(abi.encodePacked(_index))].addUser(_user);
    }

    function removeUserFromDataWhiteList(uint256 _index, address _user) external onlyDataOwner(_index) returns (bool) {
        require(dataWhiteList[keccak256(abi.encodePacked(_index))].contains(_user), "The user does not exist");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].remove(_user);
    }

    function getUserSizeOfDataWhiteList(uint256 _index) external view returns (uint256) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].length();
    }

    function getUserFromDataWhiteList(uint256 _index) external view returns (address) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].at(_index);
    }
}