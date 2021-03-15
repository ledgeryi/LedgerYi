// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

import "./StorageManager.sol";

contract NodeManager {

    event NodeAdded(address owner, string host, uint32 port);
    event NodeUpdated(address owner, string host, uint32 port);
    event NodeDeleted(address owner, string host, uint32 port);

    uint32 public numberOfNodes;

    StorageManager private storageManager;

    constructor(address _storage) public {
        storageManager = StorageManager(_storage);
    }

    function addNode(address _owner, string memory _host, uint32 _port) public {
        bytes32 _record = keccak256(abi.encode(numberOfNodes));
        numberOfNodes++;
        storageManager.pushNode( _record, _owner, _host, _port);
        emit NodeAdded(_owner, _host, _port);
    }

    function updateNode(bytes32 _record, address _owner, string memory _host, uint32 _port) public {
        storageManager.updateNode(_record, _host, _port);
        emit NodeUpdated(_owner, _host, _port);
    }

    function deleteNode(bytes32 _record) public {
        address owner;
        string memory host;
        uint32 port;
        (,owner,host,port) = storageManager.nodeInfo(_record);
        storageManager.removeNode(_record);
        numberOfNodes--;
        emit NodeDeleted(owner, host, port);
    }

    function getNode(uint32 index) public view returns(bytes32, address, string memory, uint32) {
        bytes32 _record = keccak256(abi.encode(index));
        return storageManager.nodeInfo(_record);
    }
}