// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

contract StorageManager {

    /*struct Committee {
        address managerId;
        uint256 status;
        // 1 - Approval    "Proposal to add to list"
        // 2 - Active      "Get enough votes"
        // 3 - Suspended   "Can be activated"
        // 4 - Inactive    "Life cycle end"
    }*/

    struct Role {
        uint32 roleId;
        // 1 - read only
        // 2 - contract call
        // 3 - contract deploy
        // 4 - block produce
        bool active;
        //mapping (address => bool) users;
    }

    struct User{
        bytes32 id;
        uint32 roleId;
        address user;
        bool active;
    }

    struct Node {
        bytes32 id;
        address owner;
        string host; //127.0.0.1
        uint32 port; //50055
        //uint32 status;
        // 1 - Approval    "Proposal to add to list"
        // 2 - Active      "Get enough votes"
        // 3 - Suspended   "Can be activated"
        // 4 - Inactive    "Life cycle end"
        bool exist;
    }

    //Committee[] private committeeList;

    Role[] private roleStorage;

    mapping(bytes32 => User) private userStorage;

    //mapping(bytes32 => Role) private roleStorage;

    mapping(bytes32 => Node) private nodeStorage;

    modifier roleNotExist(uint32 _roleId) {
        require(checkRoleExist(_roleId) == false, "Role has already exist");
        _;
    }

    modifier roleExist(uint32 _roleId) {
        require(checkRoleExist(_roleId) == true, "Role does not exist");
        _;
    }

    function pushNode(bytes32 _record, address _owner, string memory _host, uint32 _port) external {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == false, "node already exist");
        nodeStorage[_record] = Node(_record, _owner, _host, _port, true);
    }

    function updateNode(bytes32 _record, string memory _host, uint32 _port) external {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == true, "node does not exist");
        nodeStorage[_record] = Node(tmp.id, tmp.owner, _host, _port, true);
    }

    function removeNode(bytes32 _record) external {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == true, "node does not exist");
        delete nodeStorage[_record];
    }

    function nodeInfo(bytes32 _record) external view returns(bytes32, address, string memory, uint32) {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == true, "node does not exist");
        return (_record, tmp.owner, tmp.host, tmp.port);
    }

    function pushRole(uint32 _roleId) external roleNotExist(_roleId) returns(uint256) {
        roleStorage.push(Role(_roleId, true));
        return roleStorage.length;
    }

    function roleNum() external view returns(uint256){
        return roleStorage.length;
    }

    function roleInfo(uint256 _index) external view returns(uint32, bool){
        return (roleStorage[_index].roleId, roleStorage[_index].active);
    }

    function checkRoleExist(uint32 _roleId) private view returns (bool) {
        uint256 length = roleStorage.length;
        for(uint i = 0; i < length; i ++){
            bool exist = roleStorage[i].roleId == _roleId;
            if (exist) {
                return true;
            }
        }
        return false;
    }

    function inactiveRole(uint32 _roleId) external returns(bool) {
        uint256 length = roleStorage.length;
        for(uint256 i = 0; i < length; i ++){
            bool exist = roleStorage[i].roleId == _roleId;
            if (exist) {
                roleStorage[i].active = false;
                return true;
            }
        }
        return false;
    }

    function roleState(uint32 _roleId) private view returns(bool) {
        uint256 length = roleStorage.length;
        for(uint256 i = 0; i < length; i ++){
            bool exist = roleStorage[i].roleId == _roleId;
            if (exist) {
                return roleStorage[i].active;
            }
        }
        return false;
    }

    function roleAssigned(bytes32 _record, uint32 _roleId, address _user) external view roleExist(_roleId) returns (bool) {
        require(userStorage[_record].roleId == _roleId, "roleId inconsistency");
        require(userStorage[_record].user == _user, "user address inconsistency");
        return userStorage[_record].active;
    }

    function revocationUser(bytes32 _record, uint32 _roleId, address _user) external roleExist( _roleId) {
        require(userStorage[_record].roleId == _roleId, "roleId inconsistency");
        require(userStorage[_record].user == _user, "user address inconsistency");
        userStorage[_record].active = false;
    }

    function assignRole(bytes32 _record, uint32 _roleId, address _user) external roleExist(_roleId) {
        require(roleState(_roleId) == true, "role state is inactive");
        userStorage[_record] = User(_record, _roleId, _user, true);
    }

    function userInfo(bytes32 _record) external view returns(bytes32, uint32, address, bool) {
        User storage tmp = userStorage[_record];
        return (tmp.id, tmp.roleId, tmp.user, tmp.active);
    }
}
