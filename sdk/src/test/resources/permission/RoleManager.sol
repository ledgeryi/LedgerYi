// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

import "./StorageManager.sol";

contract RoleManager {

    event RoleCreated(uint32 roleId);
    event RoleRevoked(uint32 roleId);
    event UserAdded(address account, uint32 roleId);
    event UserRemoved(address account, uint32 roleId);

    StorageManager private storageManager;

    uint256 public numberOfUsers;

    uint256 public numberOfRoles;

    constructor(address _storage) public {
        storageManager = StorageManager(_storage);
    }

    function init() public {
        numberOfRoles = storageManager.roleNum();
    }

    function addRole(uint32 _roleId) public {
        numberOfRoles = storageManager.pushRole(_roleId);
        emit RoleCreated(_roleId);
    }

    function getRole(uint256 _index) public view returns(uint32, bool){
        return storageManager.roleInfo(_index);
    }

    function revokeRole(uint32 _roleId) public returns(bool){
        bool result = storageManager.inactiveRole(_roleId);
        emit RoleRevoked(_roleId);
        return result;
    }

    function addUser(uint32 _roleId, address _user) public {
        storageManager.assignRole(keccak256(abi.encode(numberOfUsers)), _roleId, _user);
        numberOfUsers++;
        emit UserAdded(_user, _roleId);
    }

    function removeUser(bytes32 id, uint32 _roleId, address _user) public {
        storageManager.revocationUser(id, _roleId, _user);
        emit UserRemoved(_user, _roleId);
    }

    function hasRole(bytes32 _id, uint32 _roleId, address _user) public view returns (bool){
        return storageManager.roleAssigned(_id, _roleId, _user);
    }

    function getUser(uint256 _index) public view returns(bytes32, uint32, address, bool) {
        return storageManager.userInfo(keccak256(abi.encode(_index)));
    }

}