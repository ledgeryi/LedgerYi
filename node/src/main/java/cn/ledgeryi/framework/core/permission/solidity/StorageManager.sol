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
        mapping (address => bool) accounts;
    }

    struct Node {
        address nodeOwner;
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

    mapping(bytes32 => Role) private roleStorage;

    mapping(bytes32 => Node) private nodeStorage;

    modifier roleExists(bytes32 _record, uint32 _roleId) {
        require(roleStorage[_record].roleId == _roleId, "roleId inconsistency");
        _;
    }

    modifier nodeExists(bytes32 _record, string memory _host, uint32 _port) {
        require(keccak256(abi.encode(nodeStorage[_record].host)) == keccak256(abi.encode(_host)), "host inconsistency");
        require(nodeStorage[_record].port == _port, "port inconsistency");
        _;
    }

    function pushNode(bytes32 _record, address _owner, string memory _host, uint32 _port) external {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == false, "node already exist");
        nodeStorage[_record] = Node(_owner, _host, _port, true);
    }

    function nodeInfo(bytes32 _record) external view returns(address, string memory, uint32) {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == true, "node does not exist");
        return (tmp.nodeOwner, tmp.host, tmp.port);
    }

    function pushRole(bytes32 _record, uint32 _roleId) external {
        roleStorage[_record] = Role(_roleId, true);
    }

    function inactiveRole(bytes32 _record, uint32 _roleId) external roleExists(_record, _roleId) {
        roleStorage[_record].active = false;
    }

    function roleAssigned(bytes32 _record, uint32 _roleId, address _account) external view roleExists(_record, _roleId) returns (bool) {
        require(roleStorage[_record].roleId == _roleId, "roleId inconsistency");
        return roleStorage[_record].accounts[_account];
    }

    function revocationAccount(bytes32 _record, uint32 _roleId, address _account) external roleExists(_record, _roleId) {
        require(roleStorage[_record].roleId == _roleId, "roleId inconsistency");
        roleStorage[_record].accounts[_account] = false;
    }

    function assignRole(bytes32 _record, uint32 _roleId, address _account) external roleExists(_record, _roleId) {
        roleStorage[_record].accounts[_account] = true;
    }
}
