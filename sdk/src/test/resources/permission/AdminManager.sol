// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

import "./AdminStorage.sol";

contract AdminManager {
    uint8 private ENABLE = 1;
    uint8 private DISABLE = 2;

    AdminStorage private adminStorage;

    address private proposalManager;

    constructor(address _adminStorage) public{
        adminStorage = AdminStorage(_adminStorage);
    }

    //protect add/remove admin
    modifier onlyProposalManager() {
        require(proposalManager == msg.sender, "only proposal manager have right to call it");
        _;
    }

    function init(address _proposalManager) public{
//        require(proposalManager == address(0), "you cannot reset proposalManager");
        proposalManager = _proposalManager;
    }

    function addAdmin(address _user) public onlyProposalManager {
        adminStorage.addAdmin(_user, ENABLE);
    }

    function enableAdmin(address _user) public {
        adminStorage.enableAdmin(_user);
    }

    function disableAdmin(address _user) public {
        adminStorage.disableAdmin(_user);
    }

    function beActiveAdmin(address _user) public view returns (bool){
        return adminStorage.beActiveAdmin(_user);
    }

    function removeAdmin(address _user) public onlyProposalManager{
        adminStorage.removeAdmin(_user);
    }

    function beAdmin(address _user) public view returns (bool){
        return adminStorage.beAdmin(_user);
    }

    function queryAllAdminInfo() public view returns (AdminStorage.Admin[] memory) {
        return adminStorage.queryAllAdminInfo();
    }

    function queryAdminInfo(address _owner) public view returns (address, uint8) {
        return adminStorage.queryAdminInfo(_owner);
    }
    function adminCount() public view returns (uint32) {
        return adminStorage.adminCount();
    }
}