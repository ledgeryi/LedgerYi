// SPDX-License-Identifier: GPL-3.0

pragma solidity ^0.6.8;

import "./EnumerableSet.sol";

contract TraceSourceProxy {

    using EnumerableSet for EnumerableSet.StringSet;

    // struct TraceLink {
    //     string tractLinkName;
    //     address[] traceContract;
    // }

    address private owner;

    string private uid;

    string private nameZn;

    string private nameEn;

    //TraceLink[] internal traceLinks;

    //string[] private traceLinkNames;

    EnumerableSet private traceLinkNames;

    mapping(bytes32 => address[]) private traceLinks;

    modifier onlyOwner() {
        require(msg.sender == owner, "Caller is not owner");
        _;
    }

    constructor (string memory _nameZn, string memory _nameEn, string memory _uid) public {
        owner = msg.sender;
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

    function getTraceLinkLength() external view returns(int32) {
        return traceLinkNames.length;
    }

    function getTraceLinkName(int32 _index) external view returns(string memory) {
        return traceLinkNames.at(_index);
    }

    function getTraceContractSize(string memory _linkName) external view returns(int32) {
        return traceLinks[keccak256(_linkName)].length;
    }

    function getTraceContractAddress(string memory _linkName, int32 _index) external view returns(address) {
        return traceLinks[keccak256(_linkName)][_index];
    }

    function addTraceLink(string memory _linkName, address _traceContract) onlyOwner external returns (bool) {
        if (!traceLinkNames.contains(_linkName)) {
            traceLinkNames.add(_linkName);
            traceLinks[keccak256(_linkName)].push(_traceContract);
            return true;
        }
        return false;
    }

    // function addTraceLink(string memory _linkName, address _contractName) public onlyOwner {
    //     TraceLink memory _traceLink = TraceLink(_linkName, _contractName);
    //     traceLinks.push(_traceLink);
    // }

}