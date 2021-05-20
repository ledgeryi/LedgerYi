// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

import "./NodeStorage.sol";

contract NodeManager {

    event NodeAdded(address owner, string host, uint16 port);
    event NodeUpdated(address owner, string host, uint16 port);
    event NodeStatusUpdated(address owner, uint8 status);
    event NodeDeleted(address owner, string host, uint16 port);

    NodeStorage private nodeStorage;
    uint8 constant internal ENABLE = 1;
    uint8 constant internal DISABLE = 2;

    address private proposalManager;

    constructor(address _nodeStorage) public {
    nodeStorage = NodeStorage(_nodeStorage);
    }

    //protect add/remove node
    modifier onlyProposalManager() {
        require(proposalManager == msg.sender, "only proposal manager have right to call it");
        _;
    }

    function init(address _proposalManager) public{
//        require(proposalManager == address(0), "you cannot reset proposalManager");
        proposalManager = _proposalManager;
    }

    modifier nodeExists(address _owner) {
        require(nodeStorage.nodeExist(_owner), "address haven not register");
        _;
    }

    modifier nodeNotExists(address _owner) {
        require(nodeStorage.nodeNotExist(_owner), "address already register");
        _;
    }

    function addEnabledNode(address _owner, string memory _host, uint16 _port) public nodeNotExists(_owner) {
        addNode(_owner, _host, _port, ENABLE);
    }

    function addDisabledNode(address _owner, string memory _host, uint16 _port) public nodeNotExists(_owner) {
        addNode(_owner, _host, _port, DISABLE);
    }

    function addNode(address _owner, string memory _host, uint16 _port, uint8 _status) internal onlyProposalManager nodeNotExists(_owner) {
        nodeStorage.createNode(_owner, _host, _port, _status);

        emit NodeAdded(_owner, _host, _port);
    }

    function enableNode(address _owner) public nodeExists(_owner) {
        require(!beEnabledNode(_owner), "duplicate enable");
        nodeStorage.updateNodeStatus(_owner, 1);
        emit NodeStatusUpdated(_owner, 1);
    }

    function disableNode(address _owner) public nodeExists(_owner) {
        require(beEnabledNode(_owner), "duplicate disable");
        nodeStorage.updateNodeStatus(_owner, 2);
        emit NodeStatusUpdated(_owner, 2);
    }

    function beEnabledNode(address _owner) internal view returns (bool) {
        uint8 _status;
        (,,, _status) = queryNodeInfo(_owner);

        return _status == 1;
    }

    function removeNode(address _owner) public nodeExists(_owner) onlyProposalManager{
        string memory _bindHost;
        uint16 _bindPort;
        (, _bindHost, _bindPort,) = queryNodeInfo(_owner);

        nodeStorage.removeNode(_owner);
        emit NodeDeleted(_owner, _bindHost, _bindPort);
    }

    function queryNodeInfo(address _owner) public view returns (address, string memory, uint16, uint8) {
        require(nodeStorage.nodeExist(_owner), "node does not exist");
        return nodeStorage.queryNodeInfoByAddress(_owner);
    }

    function beExistsDuplicateHostAndPort(string memory _host, uint16 _port) public view returns (bool) {
        uint16 _existPort;
        (,, _existPort,) = nodeStorage.queryNodeInfoByHostAndPort(_host, _port);

        return _existPort > 0;
    }

    function calculateNodeIndexId(string memory _host, uint16 _port) internal pure returns (bytes32){
        return keccak256(abi.encodePacked(_host, _port));
    }

    function queryAllNodeInfo() public view returns (Node[] memory) {
        return convert(nodeStorage.queryAllNodeInfo());
    }

    function convert(NodeStorage.Node[] memory originResults) internal pure returns (Node[] memory) {
        uint len = originResults.length;
        Node[] memory results = new Node[](len);

        for (uint32 i = 0; i < len; i++) {
            results[i] = Node(originResults[i].owner, originResults[i].host, originResults[i].port, originResults[i].status);
        }

        return results;
    }

    function queryAllNodeInfoByPage(uint32 start, uint8 fetchCount) public view returns (Node[] memory) {
        uint32 size = nodeStorage.nodeCount();
        if (size <= 0 || start >= size || fetchCount < 1 || fetchCount > 100) {
            return new Node[](0);
        }
        if (size < fetchCount) {
            fetchCount = uint8(size);
        }
        return convert(nodeStorage.queryAllNodeInfoByPage(start, fetchCount));
    }

    struct Node {
        address owner;
        string host;
        uint16 port;
        uint8 status;
    }
}