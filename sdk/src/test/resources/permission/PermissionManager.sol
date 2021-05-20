// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

import "./PermissionStorage.sol";

contract PermissionManager {
    uint8 private ENABLE = 1;
    uint8 private DISABLE = 2;

    event RoleCreated(uint32 roleId);
    event RoleRevoked(uint32 roleId);
    event UserAdded(address account, uint32 roleId);
    event UserUpdated(address account, uint32 roleId);
    event UserRemoved(address account, uint32 roleId);

    PermissionStorage private permissionStorage;

    constructor(address _permissionStorage) public {
        permissionStorage = PermissionStorage(_permissionStorage);
    }

    modifier roleIdExist(uint8 _roleId) {
        require(permissionStorage.beRoleExistById(_roleId), "role does not exist");
        _;
    }

    modifier permissionExist(address _addr) {
        require(permissionStorage.beRoleBindWithUser(_addr), "permission does not exist");
        _;
    }

    modifier userExist(address _addr) {
        require(permissionStorage.beUserExist(_addr), "user have not register permission");
        _;
    }

    modifier userNotExist(address _addr) {
        require(!permissionStorage.beUserExist(_addr), "duplicate permission for user");
        _;
    }

    function getRole(uint8 _roleId) public roleIdExist(_roleId) view returns (uint8, bytes32){
        return permissionStorage.queryRoleInfo(_roleId);
    }

    function createUser(address _user, uint8 _roleId) public roleIdExist(_roleId) userNotExist(_user) {
        permissionStorage.createPermission(_user, _roleId);
        emit UserAdded(_user, _roleId);
    }

    function updateUserRole(address _user, uint8 _roleId) public roleIdExist(_roleId) userExist(_user) permissionExist(_user) {
        permissionStorage.updatePermission(_user, _roleId);
        emit UserUpdated(_user, _roleId);
    }

    function removeUser(address _addr) public userExist(_addr){
        uint8 _roleId;
        (,_roleId,) = permissionStorage.queryUserInfo(_addr);
        permissionStorage.deleteUser(_addr);
        emit UserRemoved(_addr, _roleId);
    }

    function hasRole(address _user, uint8 _roleId) public roleIdExist(_roleId) userExist(_user) view returns (bool){
        if(!permissionStorage.beRoleExistById(_roleId)) {
            return false;
        }

        return permissionStorage.hasActiveRole(_user, _roleId);
    }

    function disableRole(address _addr) public permissionExist(_addr) {
        require(permissionStorage.checkRoleStatus(_addr), "repeat disable");
        permissionStorage.disableRole(_addr);
    }

    function enableRole(address _addr) external permissionExist(_addr) {
        require(!permissionStorage.checkRoleStatus(_addr), "repeat enable");
        permissionStorage.enableRole(_addr);
    }

    function queryUserInfo(address _addr) public view returns (address, uint8, bool) {
        require(permissionStorage.beUserExist(_addr), "user does not registe");
        return permissionStorage.queryUserInfo(_addr);
    }

    function queryAllUserInfo() public view returns (PermissionStorage.User[] memory) {
        return permissionStorage.queryAllUserInfo();
    }

    function queryAllUserInfoByPage(uint32 start, uint8 fetchCount) public view returns (PermissionStorage.User[] memory) {
        uint32 size = permissionStorage.userCount();
        if(size <= 0 || start >= size || fetchCount < 1 || fetchCount > 100) {
            return new PermissionStorage.User[](0);
        }

        if(size < fetchCount) {
            fetchCount = uint8(size);
        }

        return permissionStorage.queryAllUserInfoByPage(start, fetchCount);
    }

    function userCount() public view returns (uint32) {
        return permissionStorage.userCount();
    }
}