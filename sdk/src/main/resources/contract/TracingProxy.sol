// SPDX-License-Identifier: GPL-3.0

pragma solidity ^0.6.9;

import "./StringSet.sol";
import "./AddressSet.sol";
import "./Tracing.sol";

contract TracingProxy {

    using StringSet for StringSet.Set;
    using AddressSet for AddressSet.Set;

    address private owner;

    string private uid;

    string private nameZn;

    string private nameEn;

    uint private createTime;

    StringSet.Set private traceLinkNames;

    mapping(bytes32 => AddressSet.Set) private traceLinks;

    modifier onlyOwner() {
        require(msg.sender == owner, "Caller is not owner");
        _;
    }

    constructor (string memory _nameZn, string memory _nameEn, string memory _uid) public {
        owner = msg.sender;
        createTime = now;
        nameEn = _nameEn;
        nameZn = _nameZn;
        uid = _uid;
    }

    function getOwner() external view returns (address) {
        return owner;
    }

    function getUid() external view returns(string memory) {
        return uid;
    }

    function getName() external view returns(string memory, string memory) {
        return (nameZn, nameEn);
    }

    function getCreateTime() external view returns (uint) {
        return createTime;
    }

    function getTraceLinkLength() external view returns(uint256) {
        return traceLinkNames.length();
    }

    function getTraceLinkName(uint256 _index) external view returns(string memory) {
        return traceLinkNames.at(_index);
    }

    function getTraceContractSize(string memory _linkName) external view returns(uint256) {
        return traceLinks[keccak256(abi.encodePacked(_linkName))].length();
    }

    function getTraceContractAddress(string memory _linkName, uint256 _index) external view returns(address) {
        return traceLinks[keccak256(abi.encodePacked(_linkName))].at(_index);
    }

    function addTraceLink(string memory _linkName, address _traceContract) onlyOwner external returns (bool) {
        traceLinkNames.add(_linkName);
        Tracing tracing = Tracing(_traceContract);
        string memory _uid = tracing.getUid();
        require(keccak256(abi.encodePacked(uid)) == keccak256(abi.encodePacked(_uid)), "Uid inconformity");
        return traceLinks[keccak256(abi.encodePacked(_linkName))].add(_traceContract);
    }

}