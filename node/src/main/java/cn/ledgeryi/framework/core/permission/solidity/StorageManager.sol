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
        bytes32 roleName;
        // 1 - read only
        // 2 - contract call
        // 3 - contract deploy
        // 4 - block produce
    }

    struct User {
        bytes32 id;
        uint32 roleId;
        address user;
        bool active;
    }

    struct Admin {
        bytes32 id;
        address user;
        bool beExists;
    }

    struct Node {
        bytes32 id;
        address owner;
        string host; //127.0.0.1
        uint32 port; //50055
    }

    struct Topic {
        bytes32 id;
        uint start;
        uint end;
        uint8 operatorType;
        uint16 allVoteCount;
        uint16 voteCount;
        uint8 status; //1:WaitApproval, 2:Approval, 3:Suspended
    }

    uint8 private constant Topic_WaitApproval = 1;

    struct NodeProposal {
        Topic topic;
        address owner;
        bytes32 host;
        uint32 port;
    }

    //admin only for manage the user
    struct AdminProposal {
        Topic topic;
        uint32 roleId;
        address user;
    }

    struct Voter {
        bytes32 id;
        address admin;
        bytes32 proposalId;
        bool beVote;
    }
    //Committee[] private committeeList;

    Role[] private roleStorage = new Role[](4);

    mapping(bytes32 => User) private userStorage;

    //mapping(bytes32 => Role) private roleStorage;

    mapping(bytes32 => Node) private nodeStorage;
    mapping(bytes32 => Admin) private adminStorage;
    mapping(bytes32 => NodeProposal) private nodeProposalStorage;

    modifier roleNotExist(uint32 _roleId) {
        require(checkRoleExist(_roleId) == false, "Role has already exist");
        _;
    }

    modifier roleExist(uint32 _roleId) {
        require(checkRoleExist(_roleId) == true, "Role does not exist");
        _;
    }

    modifier onlyForAdmin(bytes32 _adminId) {
        require(adminStorage[_adminId].beExists == true, "Only allow for admin");
        _;
    }

    constructor() public {
        initInbuiltRole();
    }

    function initInbuiltRole() internal {
        if (roleStorage.length == 4) {
            return;
        }
        roleStorage[0] = Role(1, 'Read Only');
        roleStorage[1] = Role(2, 'Contract Call');
        roleStorage[2] = Role(3, 'Contract Deploy');
        roleStorage[3] = Role(4, 'Block Produce');
    }

    function checkRoleExist(uint32 _roleId) private view returns (bool) {
        return _roleId >= 0 && _roleId < roleStorage.length;
    }

    function queryRoleSize() external view returns (uint256){
        return roleStorage.length;
    }

    function queryRoleInfo(uint256 _index) external view returns (uint32, bytes32){
        return (roleStorage[_index].roleId, roleStorage[_index].roleName);
    }

    function beHasActiveRole(bytes32 _record, uint32 _roleId, address _user) external view roleExist(_roleId) returns (bool) {
        require(userStorage[_record].roleId == _roleId, "roleId inconsistency");
        require(userStorage[_record].user == _user, "user address inconsistency");
        return userStorage[_record].active;
    }

    function assignRole(bytes32 _record, uint32 _roleId, address _user) external roleExist(_roleId) {
        //update role when exists
        if (userStorage[_record].user != 0) {
            userStorage[_record].active = true;
            userStorage[_record].roleId = _roleId;
            return;
        }
        //create if doest exists
        userStorage[_record] = User(_record, _roleId, _user, true);
    }

    function deleteUser(bytes32 id) external roleExist(_roleId) {
        address userAddress = userStorage[id].user;

        if (userAddress != 0) {
            delete userStorage[id];
        }
    }

    function queryUserInfo(bytes32 _record) external view returns (bytes32, uint32, address, bool) {
        //wait confirm
        User storage tmp = userStorage[_record];
        return (tmp.id, tmp.roleId, tmp.user, tmp.active);
    }

    function addNode(bytes32 _record, address _owner, string memory _host, uint32 _port) external {
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

    function queryNodeInfo(bytes32 _record) external view returns (bytes32, address, string memory, uint32) {
        Node storage tmp = nodeStorage[_record];
        require(tmp.exist == true, "node does not exist");
        return (_record, tmp.owner, tmp.host, tmp.port);
    }

    //need distinct
    function createNodeProposal(bytes32 id, uint _start, uint _end, uint8 _operatorType, uint16 _allVoteCount, uint16 _voteCount,
        address _owner, bytes32 _host, uint32 _port) external onlyForAdmin(_adminId){
        //common check
        require(_start > 0 , "_start must great than 0");
        require(_start <= _end , "_end must great than _start");
        require(_operatorType > 0 , "_operatorType must great than 0");
        require(_allVoteCount > 0 , "_allVoteCount must great than 0");
        require(_port > 0 , "_port must great than 0");

        if(nodeProposalStorage[id].topic.end > 0) {
            return;
        }

        Topic topic = Topic(id, _start, _end, _operatorType, _allVoteCount, 0, Topic_WaitApproval);
        proposal[id] = NodeProposal(topic, _owner, _host, _port);
    }

    function voteNodeProposal(bytes32 _adminId,address _owner) external onlyForAdmin(_adminId){
        if(nodeProposalStorage[_adminId].topic.end > 0) {
            return;
        }

        Topic topic = Topic(_adminId, _start, _end, _operatorType, _allVoteCount, 0, Topic_WaitApproval);
        proposal[_adminId] = NodeProposal(topic, _owner, _host, _port);
    }
}
