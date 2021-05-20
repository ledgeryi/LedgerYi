// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

contract NodeStorage {
    struct Node {
        bytes32 indexId; // code(host + port) avoid repeat
        address owner;
        string host; //127.0.0.1
        uint16 port; //50055
        uint8 status;
        // 1 - enable
        // 2 - disable
    }

    uint8 constant internal ENABLE = 1;
    uint8 constant internal DISABLE = 2;

    mapping(bytes32 => address) internal nodeIndexStorage;
    mapping(address => Node) internal nodeStorage;

    mapping(address => mapping(bool => address)) internal nodeDLLIndex;
    bool constant internal PREV = false;
    bool constant internal NEXT = true;
    address internal ZERO = address(0);
    uint32 internal size = 0;

    function calculateNodeIndexId(string memory _host, uint16 _port) internal pure returns (bytes32){
        return keccak256(abi.encodePacked(_host, _port));
    }

    function createNode(address _owner, string memory _host, uint16 _port, uint8 _status) external {
        bytes32 _indexId = calculateNodeIndexId(_host, _port);

        require(nodeNotExist(_owner), "node already exist");
        require(nodeNotIndexExist(_indexId), "host and port are using by other node");

        nodeStorage[_owner] = Node(_indexId, _owner, _host, _port, statusConvert(_status));
        addNodeIndex(_indexId, _owner);
    }

    function addNodeIndex(bytes32 _indexId, address _owner) internal {
        nodeDLLIndex[_owner][PREV] = ZERO;
        nodeDLLIndex[_owner][NEXT] = nodeDLLIndex[ZERO][NEXT];
        nodeDLLIndex[nodeDLLIndex[ZERO][NEXT]][PREV] = _owner;
        nodeDLLIndex[ZERO][NEXT] = _owner;

        nodeIndexStorage[_indexId] = _owner;
        size++;
    }

    function removeNode(address _owner) external {
        require(nodeExist(_owner), "node does not exist");
        require(nodeIndexExist(nodeStorage[_owner].indexId), "node does not exist");

        deleteNodeIndex(nodeStorage[_owner].indexId, _owner);
        delete nodeStorage[_owner];
    }

    function deleteNodeIndex(bytes32 _indexId, address _owner) internal {
        nodeDLLIndex[nodeDLLIndex[_owner][PREV]][NEXT] = nodeDLLIndex[_owner][NEXT];
        nodeDLLIndex[nodeDLLIndex[_owner][NEXT]][PREV] = nodeDLLIndex[_owner][PREV];

        delete nodeDLLIndex[_owner][PREV];
        delete nodeDLLIndex[_owner][NEXT];

        delete nodeIndexStorage[_indexId];
        size--;
    }

    function updateNodeStatus(address _owner, uint8 _status) external {
        nodeStorage[_owner].status = statusConvert(_status);
    }

    function statusConvert(uint8 _status) internal pure returns (uint8) {
        if (_status == ENABLE) {
            return ENABLE;
        } else {
            return DISABLE;
        }
    }

    function nodeExist(address _owner) public view returns (bool){
        return nodeStorage[_owner].status != 0;
    }

    function nodeNotExist(address _owner) public view returns (bool){
        return nodeStorage[_owner].owner == address(0);
    }

    function nodeIndexExist(bytes32 _nodeId) internal view returns (bool){
        return nodeIndexStorage[_nodeId] != address(0);
    }

    function nodeNotIndexExist(bytes32 _nodeId) internal view returns (bool){
        return !nodeIndexExist(_nodeId);
    }

    function queryNodeInfoByHostAndPort(string memory host, uint16 port) external view returns (address, string memory, uint16, uint8) {
        address owner = nodeIndexStorage[calculateNodeIndexId(host, port)];
        if(owner == address(0)) {
            return (address(0), "", 0 , 0);
        }
        Node storage tmp = nodeStorage[owner];

        return (tmp.owner, tmp.host, tmp.port, tmp.status);
    }

    function queryNodeInfoByAddress(address _owner) external view returns (address, string memory, uint16, uint8) {
        Node storage tmp = nodeStorage[_owner];

        return (tmp.owner, tmp.host, tmp.port, tmp.status);
    }

    function queryAllNodeInfo() public view returns (Node[] memory) {
        if (size < 1) {
            return new Node[](size);
        }
        Node[] memory result = new Node[](size);
        address current = nodeDLLIndex[ZERO][NEXT];
        uint8 index = 0;

        while (current != ZERO) {
            result[index++] = nodeStorage[current];
            current = nodeDLLIndex[current][NEXT];
        }

        return result;
    }

    function queryAllNodeInfoByPage(uint32 start, uint8 fetchCount) public view returns (Node[] memory) {
        Node[] memory result = new Node[](fetchCount);

        address current = nodeDLLIndex[ZERO][NEXT];
        uint8 index = 0;

        while (current != ZERO && index < start) {
            current = nodeDLLIndex[current][NEXT];
            index++;
        }

        index = 0;
        while (current != ZERO && index < fetchCount) {
            result[index++] = nodeStorage[current];
            current = nodeDLLIndex[current][NEXT];
        }
        return result;
    }

    function nodeCount() public view returns (uint32) {
        return size;
    }
}