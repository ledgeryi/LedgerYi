// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;

pragma solidity ^0.6.9;

contract ProposalStorage {
    struct Topic {
        bytes32 id;
        uint start;
        uint end;
        uint8 operatorType;
        uint allVoteCount;
        uint voteCount;
        bool beImplements;
        address owner;
    }

    struct NodeProposal {
        Topic topic;
        string host;
        uint16 port;
    }

    struct AdminProposal {
        Topic topic;
        address admin;
    }

    struct Voter {
        bytes32 proposalId;
        address voter;
        bool beVote;
    }

    mapping(bytes32 => NodeProposal) nodeProposalIndexStorage;
    mapping(bytes32 => AdminProposal) adminProposalIndexStorage;
    mapping(bytes32 => Voter) voterStorage;

    function createNodeProposal(bytes32 proposalId, uint _start, uint _end, uint8 _operatorType, uint _allVoteCount, address _proposer,
        string memory _host, uint16 _port) public returns (NodeProposal memory){
        nodeProposalCreateCheck(_start, _end, _allVoteCount, _proposer, proposalId);

        Topic memory _topic = Topic({id : proposalId, start : _start, end : _end,
        operatorType : _operatorType, allVoteCount : _allVoteCount, voteCount : 0,
        beImplements : false, owner : _proposer});

        NodeProposal memory nodeProposal = NodeProposal({topic : _topic, host : _host, port : _port});
        nodeProposalIndexStorage[proposalId] = nodeProposal;

        return nodeProposal;
    }

    function nodeProposalCreateCheck(uint _start, uint _end, uint _allVoteCount, address _proposer,
        bytes32 proposalId) internal view {
        proposalCreateCheck(_start, _end, _allVoteCount, _proposer, proposalId, true);
    }

    //find node proposal for detail info
    function findNodeProposal(bytes32 proposalId) public view returns (bytes32, uint, uint, uint, uint, uint, bool, address,
        string memory, uint16){
        NodeProposal memory nodeProposal = nodeProposalIndexStorage[proposalId];
        return (nodeProposal.topic.id, nodeProposal.topic.start, nodeProposal.topic.end,
        nodeProposal.topic.operatorType, nodeProposal.topic.allVoteCount, nodeProposal.topic.voteCount,
        nodeProposal.topic.beImplements, nodeProposal.topic.owner, nodeProposal.host, nodeProposal.port);
    }

    //find node proposal for detail info
    function findNodeInfo(bytes32 proposalId) public view returns (uint8, address, string memory, uint16){
        NodeProposal memory nodeProposal = nodeProposalIndexStorage[proposalId];
        return (nodeProposal.topic.operatorType, nodeProposal.topic.owner, nodeProposal.host, nodeProposal.port);
    }

    function adminProposalCreateCheck(uint _start, uint _end, uint _allVoteCount, address _proposer,
        bytes32 proposalId) internal view {
        proposalCreateCheck(_start, _end, _allVoteCount, _proposer, proposalId, false);
    }

    function proposalCreateCheck(uint _start, uint _end, uint _allVoteCount, address _proposer,
        bytes32 proposalId, bool beNodeProposal) internal view {
        require(_start < _end, "_end must be great than _start");
        require(_allVoteCount >= 1, "_allVoteCount must be great or equals 1");
        require(_proposer != address(0), "_proposer must be great or equals 1");

        if (beNodeProposal) {
            require(nodeProposalIndexStorage[proposalId].topic.owner == address(0), "node proposal duplicate");
        } else {
            require(adminProposalIndexStorage[proposalId].topic.owner == address(0), "admin proposal duplicate");
        }
    }

    function voteNodeProposal(bytes32 proposalId, address _voter) public {
        bytes32 voterId = calculateVoterId(proposalId, _voter);

        require(voterStorage[voterId].voter == address(0), "you have vote it");

        nodeProposalVoteCheck(voterId, proposalId, now);
        NodeProposal storage nodeProposal = nodeProposalIndexStorage[proposalId];

        voterStorage[voterId] = Voter(proposalId, _voter, true);
        nodeProposal.topic.voteCount++;
    }

    function nodeProposalVoteCheck(bytes32 voterId, bytes32 proposalId, uint currentTimeStamp) internal view {
        proposalVoteCommonCheck(voterId, proposalId, currentTimeStamp, true);
    }

    function adminProposalVoteCheck(bytes32 voterId, bytes32 proposalId, uint currentTimeStamp) internal view {
        proposalVoteCommonCheck(voterId, proposalId, currentTimeStamp, false);
    }

    function proposalVoteCommonCheck(bytes32 voterId, bytes32 proposalId, uint currentTimeStamp, bool beNodeProposal) internal view {
        require(!voterStorage[voterId].beVote, "you have vote it");

        address proposalOwner;
        uint end;

        if(beNodeProposal) {
            proposalOwner = nodeProposalIndexStorage[proposalId].topic.owner;
            end = nodeProposalIndexStorage[proposalId].topic.end;
            require(!beImplementForNodeProposal(proposalId), "proposal have implements no need vote");
        } else {
            proposalOwner = adminProposalIndexStorage[proposalId].topic.owner;
            end = adminProposalIndexStorage[proposalId].topic.end;
            require(!beImplementForAdminProposal(proposalId), "proposal have implements no need vote");
        }

        require(proposalOwner != address(0), "proposal does not exist");
        require(currentTimeStamp < end, "proposal has been expire");
    }

    function beNodeProposalApproval(bytes32 proposalId) public view returns (bool) {
        NodeProposal memory nodeProposal = nodeProposalIndexStorage[proposalId];
        return nodeProposal.topic.owner != address(0) && nodeProposal.topic.voteCount * 2 > nodeProposal.topic.allVoteCount;
    }

    function implementNodeProposal(bytes32 proposalId) public view {
        NodeProposal memory nodeProposal = nodeProposalIndexStorage[proposalId];
        require(nodeProposal.topic.owner != address(0), "proposalId error");
        nodeProposal.topic.beImplements = true;
    }

    //be implements for node proposal
    function beImplementForNodeProposal(bytes32 proposalId) internal view returns(bool){
        NodeProposal memory nodeProposal = nodeProposalIndexStorage[proposalId];

        return nodeProposal.topic.owner != address(0) && nodeProposal.topic.beImplements;
    }

    function beExistNodeProposalApproval(bytes32 proposalId) public view returns (bool) {
        return nodeProposalIndexStorage[proposalId].topic.owner != address(0);
    }

    function findAdminProposal(bytes32 proposalId) public view returns (bytes32, uint, uint, uint, uint, uint, bool, address, address){
        AdminProposal memory adminProposal = adminProposalIndexStorage[proposalId];
        return (adminProposal.topic.id, adminProposal.topic.start, adminProposal.topic.end,
        adminProposal.topic.operatorType, adminProposal.topic.allVoteCount, adminProposal.topic.voteCount,
        adminProposal.topic.beImplements, adminProposal.topic.owner, adminProposal.admin);
    }

    function findAdminInfo(bytes32 proposalId) public view returns (uint8, address){
        AdminProposal memory adminProposal = adminProposalIndexStorage[proposalId];
        return (adminProposal.topic.operatorType, adminProposal.admin);
    }

    function createAdminProposal(bytes32 proposalId,
        uint _start, uint _end, uint8 _operatorType, uint _allVoteCount, address _proposer,
        address _admin) public returns (AdminProposal memory){
        adminProposalCreateCheck(_start, _end, _allVoteCount, _proposer, proposalId);

        Topic memory _topic = Topic({id : proposalId, start : _start, end : _end,
        operatorType : _operatorType, allVoteCount : _allVoteCount, voteCount : 0,
        beImplements : false, owner : _proposer});
        AdminProposal memory adminProposal = AdminProposal({topic : _topic, admin : _admin});

        adminProposalIndexStorage[proposalId] = adminProposal;

        return adminProposal;
    }

    function voteAdminProposal(bytes32 proposalId, address _voter) public {
        bytes32 voterId = calculateVoterId(proposalId, _voter);

        require(voterStorage[voterId].voter == address(0), "you have vote it");

        adminProposalVoteCheck(calculateVoterId(proposalId, _voter), proposalId, now);
        AdminProposal storage adminProposal = adminProposalIndexStorage[proposalId];

        voterStorage[proposalId] = Voter(proposalId, _voter, true);
        adminProposal.topic.voteCount++;
    }

    function implementAdminProposal(bytes32 proposalId) public view{
        AdminProposal memory adminProposal = adminProposalIndexStorage[proposalId];
        require(adminProposal.topic.owner != address(0), "proposalId error");
        adminProposal.topic.beImplements = true;
    }

    //be implements for node proposal
    function beImplementForAdminProposal(bytes32 proposalId) internal view returns(bool){
        AdminProposal memory adminProposal = adminProposalIndexStorage[proposalId];

        return adminProposal.topic.owner != address(0) && adminProposal.topic.beImplements;
    }


    function bAdminProposalApproval(bytes32 proposalId) public view returns (bool) {
        AdminProposal memory adminProposal = adminProposalIndexStorage[proposalId];
        return adminProposal.topic.owner != address(0) && adminProposal.topic.voteCount * 2 > adminProposal.topic.allVoteCount;
    }

    function beExistAdminProposalApproval(bytes32 proposalId) public view returns (bool) {
        return adminProposalIndexStorage[proposalId].topic.owner != address(0);
    }

    function calculateVoterId(bytes32 proposalId, address _voter) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(proposalId, _voter));
    }
}