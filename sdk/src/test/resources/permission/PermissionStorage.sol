// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

contract PermissionStorage {
    struct Role {
        uint8 roleId;
        bytes32 roleName;
        // 1 - read only
        // 2 - contract call
        // 3 - contract deploy
        // 4 - block produce
    }

    struct User {
        address account;
        uint8 roleId;
        bool active;
    }

    Role[] private roleStorage;

    mapping(address => User) private userStorage;

    //double linked list for user
    mapping(address => mapping(bool => address)) private userDLLIndex;
    bool constant private PREV = false;
    bool constant private NEXT = true;
    address private ZERO  = address(0);
    uint32 private size = 0;

    constructor() public {
        initInbuiltRole();
    }

    function initInbuiltRole() internal {
        if (roleStorage.length == 4) {
            return;
        }
        roleStorage.push(Role(1, 'Read Only'));
        roleStorage.push(Role(2, 'Contract Call'));
        roleStorage.push(Role(3, 'Contract Deploy'));
        roleStorage.push(Role(4, 'Block Produce'));
    }

    //bind role with user, if exists will update the relation
    function createPermission(address _addr, uint8 _roleId) public {
        require(!beUserExist(_addr), "duplicate permission");
        //create if doest exists
        userStorage[_addr] = User(_addr, _roleId, true);
        addNodeIndex(_addr);
    }

    //bind role with user, if exists will update the relation
    function updatePermission(address _addr, uint8 _roleId) public {
        //update role when exists
        require(beUserExist(_addr), "permission does not exists");
        userStorage[_addr].roleId = _roleId;
    }

    function addNodeIndex(address _addr) internal {
        //new:P
        userDLLIndex[_addr][PREV] = ZERO;
        //new:N
        userDLLIndex[_addr][NEXT] = userDLLIndex[ZERO][NEXT];
        //end:P
        userDLLIndex[userDLLIndex[ZERO][NEXT]][PREV] = _addr;
        //start:N
        userDLLIndex[ZERO][NEXT] = _addr;

        size++;
    }

    function deleteUser(address _addr) public {
        delete userStorage[_addr];
        deleteNodeIndex(_addr);
    }

    function deleteNodeIndex(address _addr) internal {
        userDLLIndex[userDLLIndex[_addr][PREV]][NEXT] = userDLLIndex[_addr][NEXT];
        userDLLIndex[userDLLIndex[_addr][NEXT]][PREV] = userDLLIndex[_addr][PREV];

        delete userDLLIndex[_addr][PREV];
        delete userDLLIndex[_addr][NEXT];

        size--;
    }

    function disableRole(address _addr) public {
        userStorage[_addr].active = false;
    }

    function enableRole(address _addr) public {
        userStorage[_addr].active = true;
    }

    //check role be exists in roles
    function beRoleExistById(uint8 _roleId) public view returns (bool) {
        return _roleId >= 1 && _roleId <= roleStorage.length;
    }

    //check be bind user with role
    function beRoleBindWithUser(address _addr) public view returns (bool) {
        return beRoleExistById(userStorage[_addr].roleId);
    }

    //check user be exists in userStorage
    function beUserExist(address _addr) public view returns (bool) {
        return userStorage[_addr].account != address(0);
    }

    function queryRoleInfo(uint8 _index) external view returns (uint8, bytes32){
        return (roleStorage[_index].roleId, roleStorage[_index].roleName);
    }

    //check be bind user with role and is active
    function hasActiveRole(address _addr, uint8 _roleId) external view returns (bool) {
        return userStorage[_addr].account == _addr
        && userStorage[_addr].roleId == _roleId
        && beRoleExistById(userStorage[_addr].roleId)
        && userStorage[_addr].active;
    }

    function hasRole(address _addr) external view returns (bool) {
        return userStorage[_addr].account == _addr
        && beRoleExistById(userStorage[_addr].roleId);
    }

    //check role status is active
    function checkRoleStatus(address _addr) external view returns (bool) {
        return userStorage[_addr].active;
    }

    function queryUserInfo(address _addr) external view returns (address, uint8, bool) {
        User storage tmp = userStorage[_addr];
        return (tmp.account, tmp.roleId, tmp.active);
    }

    //consider: paging when data is more ? todo
    function queryAllUserInfo() public view returns (User[] memory) {
        if(size < 1) {
            return new User[](size);
        }
        User[] memory result = new User[](size);
        address current = userDLLIndex[ZERO][NEXT];
        uint8 index = 0;

        while (current != ZERO) {
            result[index++] = userStorage[current];
            current = userDLLIndex[current][NEXT];
        }

        return result;
    }

    function queryAllUserInfoByPage(uint32 start, uint8 fetchCount) public view returns (User[] memory) {
        User[] memory result = new User[](fetchCount);

        address current = userDLLIndex[ZERO][NEXT];
        uint8 index = 0;

        while (current != ZERO && index < start) {
            current = userDLLIndex[current][NEXT];
            index++;
        }

        index = 0;
        while (current != ZERO && index < fetchCount) {
            result[index++] = userStorage[current];
            current = userDLLIndex[current][NEXT];
        }
        return result;
    }

    function userCount() public view returns (uint32) {
        return size;
    }
}