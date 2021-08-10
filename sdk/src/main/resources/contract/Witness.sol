// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;

pragma solidity ^0.6.9;

import "./AddressSet.sol";
import "./DataSet.sol";

contract Witness {

    using AddressSet for AddressSet.WhiteList;

    using DataSet for DataSet.DataList;

    string private creater;

    string private nameZn;

    string private nameEn;

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
        require(msg.sender == dataWhiteList[keccak256(abi.encodePacked(_index))].owner, "Caller is not data owner");
        _;
    }

    constructor (string memory _creater, string memory _nameZn, string memory _nameEn) public {
        createTime = now;
        contractWhiteList.owner = msg.sender;
        creater = _creater;
        nameEn = _nameEn;
        nameZn = _nameZn;
        contractWhiteList.status = true;
    }

    function getOwner() external view returns (address) {
        return contractWhiteList.owner;
    }

    function getCreater() external view returns (string memory) {
        return creater;
    }

    function getName() external view returns (string memory, string memory) {
        return (nameZn, nameEn);
    }

    function getCreateTime() external view returns (uint) {
        return createTime;
    }

    function getBaseInfo() external view returns (string memory, string memory, string memory, uint, address) {
        return (creater, nameEn, nameZn, createTime, contractWhiteList.owner);
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
        uint256 _key =  dataList.addData(_dataInfos);
        dataWhiteList[keccak256(abi.encodePacked(_key))].setOwner(msg.sender);
        dataWhiteList[keccak256(abi.encodePacked(_key))].addUser(msg.sender);
        dataWhiteList[keccak256(abi.encodePacked(_key))].status = true;
        return _key;
    }

    function dataVerify(uint256 _index,string[] memory _dataInfos) external view returns (bool) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataList.witnessDataVerify(_index, _dataInfos);
    }

    function getDataInfo(uint256 _index) external view returns (string[] memory, string[] memory) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        require(!dataWhiteList[keccak256(abi.encodePacked(_index))].status || dataWhiteList[keccak256(abi.encodePacked(_index))].contains(msg.sender), "Caller is not in whiteList");
        return dataList.getData(_index);
    }

    function getDataInfo() external view returns (string[] memory, string[] memory) {
        uint256 _index = dataList._datas.length - 1;
        require(!dataWhiteList[keccak256(abi.encodePacked(_index))].status || dataWhiteList[keccak256(abi.encodePacked(_index))].contains(msg.sender), "Caller is not in whiteList");
        return dataList.getData(_index);
    }

    function addUserToDataWhiteList(uint256 _index, address _user) external onlyDataOwner(_index) returns (bool) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].addUser(_user);
    }

    function removeUserFromDataWhiteList(uint256 _index, address _user) external onlyDataOwner(_index) returns (bool) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        require(dataWhiteList[keccak256(abi.encodePacked(_index))].contains(_user), "The user does not exist");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].remove(_user);
    }

    function getUserSizeOfDataWhiteList(uint256 _index) external view returns (uint256) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].length();
    }

    function getUserFromDataWhiteList(uint256 _index, uint256 _userIndex) external view returns (address) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].at(_userIndex);
    }

    function disableDataWhite(uint256 _index) external onlyContractOwner {
        require(_index < dataList._datas.length, "Data index out of bounds");
        dataWhiteList[keccak256(abi.encodePacked(_index))].status = false;
    }

    function enableDataWhite(uint256 _index) external onlyContractOwner {
        require(_index < dataList._datas.length, "Data index out of bounds");
        dataWhiteList[keccak256(abi.encodePacked(_index))].status =  true;
    }

    function getStatusOfDataWhite(uint256 _index) external onlyContractOwner view returns (bool) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        return dataWhiteList[keccak256(abi.encodePacked(_index))].status;
    }
}