// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

import "./ProposalStorage.sol";
import "./NodeManager.sol";
import "./AdminManager.sol";

contract ProposalManager {
    event NodeProposalCreated(uint _start, uint _end, uint _operatorType, address _proposer, string _host, uint16 _port);

    mapping(address => ProposalStorage.NodeProposal[]) nodeProposalIndexStorage;
    mapping(address => ProposalStorage.AdminProposal[]) adminProposalIndexStorage;

    ProposalStorage internal proposalStorage;
    AdminManager internal adminManager;
    NodeManager internal nodeManager;

    uint8 internal constant NODE_ADD = 1;
    uint8 internal NODE_REMOVE = 2;

    //remain some gap for extends
    uint8 internal ADMIN_ADD = 10;
    uint8 internal ADMIN_REMOVE = 11;

    constructor (address _proposalStorage, address _adminManager, address _nodeManager) public {
        proposalStorage = ProposalStorage(_proposalStorage);
        adminManager = AdminManager(_adminManager);
        nodeManager = NodeManager(_nodeManager);
    }

    modifier nodeProposalExists(bytes32 nodeProposalId) {
        require(proposalStorage.beExistNodeProposalApproval(nodeProposalId), "node proposal does not exist");
        _;
    }
    modifier nodeProposalNotExists(bytes32 nodeProposalId) {
        require(!proposalStorage.beExistNodeProposalApproval(nodeProposalId), "node proposal duplicate");
        _;
    }

    modifier adminProposalExists(bytes32 nodeProposalId) {
        require(proposalStorage.beExistAdminProposalApproval(nodeProposalId), "admin proposal does not exist");
        _;
    }

    modifier adminProposalNotExists(bytes32 nodeProposalId) {
        require(!proposalStorage.beExistAdminProposalApproval(nodeProposalId), "admin proposal duplicate");
        _;
    }

    modifier onlyAdmin(address _owner) {
        require(adminManager.beActiveAdmin(_owner), "you have no right to create proposal");
        _;
    }

    function findAllNodeProposal(address _proposer) public view returns (ProposalStorage.NodeProposal[] memory) {
        return nodeProposalIndexStorage[_proposer];
    }

    function findNodeProposal(bytes32 nodeProposalId) public view returns (bytes32, uint, uint, uint, uint, uint, bool,
        address, string memory, uint16) {
        return proposalStorage.findNodeProposal(nodeProposalId);
    }

    function findAllAdminProposal(address proposer) public view returns (ProposalStorage.AdminProposal[] memory) {
        return adminProposalIndexStorage[proposer];
    }

    function findAdminProposal(bytes32 nodeProposalId) public view returns (bytes32, uint, uint, uint, uint, uint, bool,
        address, address) {
        return proposalStorage.findAdminProposal(nodeProposalId);
    }

    //a proposal for create node
    function createProposalWithCreateNode(uint _start, uint _end, address _proposer,
        string memory _host, uint16 _port) public onlyAdmin(_proposer) returns (bytes32){
        return processNodeProposal(_start, _end, NODE_ADD, adminManager.adminCount(), _proposer, _host, _port);
    }

    //a proposal for remove node
    function createProposalWithRemoveNode(uint _start, uint _end, address _proposer,
        string memory _host, uint16 _port) public onlyAdmin(_proposer) returns (bytes32){
        return processNodeProposal(_start, _end, NODE_REMOVE, adminManager.adminCount(), _proposer, _host, _port);
    }

    function processNodeProposal(uint _start, uint _end, uint8 _operatorType, uint _allVoteCount, address _proposer,
        string memory _host, uint16 _port) internal onlyAdmin(_proposer) returns (bytes32) {
        require(_operatorType == NODE_ADD || _operatorType == NODE_REMOVE, "illegal arguments for operatorType");
//        if (_operatorType == NODE_ADD) {
//            require(!nodeManager.beDuplicateHostAndPort(_host, _port), "due to duplicate node, case node proposal cannot be commit");
//        }

        bytes32 nodeProposalId = calculateNodeId(_start, _end, _operatorType, _proposer, _host, _port);
        require(!proposalStorage.beExistNodeProposalApproval(nodeProposalId), "duplicate node proposal");

        ProposalStorage.NodeProposal memory nodeProposal = proposalStorage.createNodeProposal(nodeProposalId, _start, _end,
            _operatorType, _allVoteCount, _proposer, _host, _port);

        //default vote for proposer
        voteNodeProposal(nodeProposalId, _proposer);
        nodeProposalIndexStorage[_proposer].push(nodeProposal);

        return nodeProposalId;
    }

    //vote a node proposal
    function voteNodeProposal(bytes32 proposalId, address _voter) public nodeProposalExists(proposalId) {
        proposalStorage.voteNodeProposal(proposalId, _voter);
        //return : voter count < (all voter count)/2
        if (!proposalStorage.beNodeProposalApproval(proposalId)) {
            return;
        }

        address _owner;
        string memory _host;
        uint16 _port;
        uint8 _operatorType;
        (_operatorType, _owner, _host, _port) = proposalStorage.findNodeInfo(proposalId);

        require(_operatorType == NODE_ADD || _operatorType == NODE_REMOVE, "illegal arguments for operatorType");
        //make implements even if fair
        proposalStorage.implementNodeProposal(proposalId);

        if (_operatorType == NODE_ADD) {
            nodeManager.addEnabledNode(_owner, _host, _port);
            return;
        }
        nodeManager.removeNode(_owner);
    }

    function beNodeProposalApproval(bytes32 proposalId) public view returns (bool) {
        require(proposalStorage.beExistNodeProposalApproval(proposalId), "node proposal does not exist");

        return proposalStorage.beNodeProposalApproval(proposalId);
    }

    function createProposalWithCreateAdmin(uint _start, uint _end, address _proposer,
        address _adminOwner) public onlyAdmin(_proposer) returns (bytes32) {
        return processAdminProposal(_start, _end, ADMIN_ADD, adminManager.adminCount(), _proposer, _adminOwner);
    }

    function createProposalWithRemoveAdmin(uint _start, uint _end, address _proposer,
        address _adminOwner) public onlyAdmin(_proposer) returns (bytes32) {
        return processAdminProposal(_start, _end, ADMIN_REMOVE, adminManager.adminCount(), _proposer, _adminOwner);
    }

    function processAdminProposal(uint _start, uint _end, uint8 _operatorType, uint _allVoteCount, address _proposer,
        address _adminOwner) internal onlyAdmin(_proposer) returns (bytes32) {
        if (_operatorType == ADMIN_ADD) {
            require(!adminManager.beAdmin(_adminOwner), "due to duplicate admin, case node proposal cannot be commit");
        } else {
            require(adminManager.beAdmin(_adminOwner), "its does not admin address");
        }
        require(_operatorType == ADMIN_ADD || _operatorType == ADMIN_REMOVE, "illegal arguments for operatorType");

        bytes32 adminProposalId = calculateAdminId(_start, _end, _operatorType, _proposer, _adminOwner);
        if (_operatorType == ADMIN_ADD) {
            require(!proposalStorage.beExistAdminProposalApproval(adminProposalId), "duplicate admin proposal");
        }

        ProposalStorage.AdminProposal memory adminProposal = proposalStorage.createAdminProposal(adminProposalId, _start, _end,
            _operatorType, _allVoteCount, _proposer, _adminOwner);

        //default vote for proposer
        voteAdminProposal(adminProposalId, _proposer);
        adminProposalIndexStorage[_proposer].push(adminProposal);

        return adminProposalId;
    }

    function voteAdminProposal(bytes32 proposalId, address _voter) public {
        proposalStorage.voteAdminProposal(proposalId, _voter);

        if (!proposalStorage.bAdminProposalApproval(proposalId)) {
            return;
        }

        uint8 _operatorType;
        address _admin;
        (_operatorType, _admin) = proposalStorage.findAdminInfo(proposalId);

        require(_operatorType == ADMIN_ADD || _operatorType == ADMIN_REMOVE, "illegal arguments for operatorType");
        //set implements even if fair
        proposalStorage.implementAdminProposal(proposalId);

        if (_operatorType == ADMIN_ADD) {
            adminManager.addAdmin(_admin);
            return;
        }

        adminManager.removeAdmin(_admin);
    }

    function bAdminProposalApproval(bytes32 proposalId) public view returns (bool) {
        require(proposalStorage.beExistAdminProposalApproval(proposalId), "admin proposal does not exist");

        return proposalStorage.bAdminProposalApproval(proposalId);
    }

    function calculateNodeId(uint _start, uint _end, uint _operatorType, address _proposer,
        string memory _host, uint16 _port) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(_start, _end, _operatorType, _proposer, _host, _port));
    }

    function calculateAdminId(uint _start, uint _end, uint _operatorType, address _proposer,
        address _adminOwner) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(_start, _end, _operatorType, _proposer, _adminOwner));
    }
}