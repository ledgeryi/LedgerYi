// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

import "./StorageManager.sol";

contract RoleManager {

    event RoleCreated(uint32 roleId);
    event RoleRevoked(uint32 roleId);
    event AccountAdded(address account, uint32 roleId);
    event AccountRemoved(address account, uint32 roleId);

    StorageManager private storageManager;

    constructor(address _storage) public {
        storageManager = StorageManager(_storage);
    }

    function addRole(uint32 _roleId) public {
        storageManager.pushRole(keccak256(abi.encode(_roleId)), _roleId);
        emit RoleCreated(_roleId);
    }

    function revokeRole(uint32 _roleId) public {
        storageManager.inactiveRole(keccak256(abi.encode(_roleId)), _roleId);
        emit RoleRevoked(_roleId);
    }

    function addAccount(address _account, uint32 _roleId) public {
        storageManager.assignRole(keccak256(abi.encode(_roleId)), _roleId, _account);
        emit AccountAdded(_account, _roleId);
    }

    function removeAccount(address _account, uint32 _roleId) public {
        storageManager.revocationAccount(keccak256(abi.encode(_roleId)), _roleId, _account);
        emit AccountRemoved(_account, _roleId);
    }

    function hasRole(address _account, uint32 _roleId) public view returns (bool){
        return storageManager.roleAssigned(keccak256(abi.encode(_roleId)), _roleId, _account);
    }

}