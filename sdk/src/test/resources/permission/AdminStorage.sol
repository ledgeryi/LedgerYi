// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;
pragma solidity ^0.6.9;

contract AdminStorage {
    uint8 private ENABLE = 1;
    uint8 private DISABLE = 2;

    struct Admin {
        address user;
        uint8 status;//1 : enable, 2: disable
    }

    mapping(address => Admin) internal adminStorage;
    mapping(address => mapping(bool => address)) internal adminDLLIndex;

    bool constant internal PREV = false;
    bool constant internal NEXT = true;
    address internal ZERO = address(0);
    uint32 internal size = 0;

    function addAdmin(address _owner, uint8 _status) public {
        require(adminStorage[_owner].user == address(0), "duplicate admin");
        adminStorage[_owner] = Admin(_owner, _status);

        adminDLLIndex[_owner][PREV] = ZERO;
        adminDLLIndex[_owner][NEXT] = adminDLLIndex[ZERO][NEXT];

        adminDLLIndex[adminDLLIndex[ZERO][NEXT]][PREV] = _owner;
        adminDLLIndex[ZERO][NEXT] = _owner;

        size++;
    }

    function removeAdmin(address _owner) public {
        require(adminStorage[_owner].user != address(0), "admin does not exists");

        adminDLLIndex[adminDLLIndex[_owner][PREV]][NEXT] = adminDLLIndex[_owner][NEXT];
        adminDLLIndex[adminDLLIndex[_owner][NEXT]][PREV] = adminDLLIndex[_owner][PREV];

        delete adminDLLIndex[_owner][PREV];
        delete adminDLLIndex[_owner][NEXT];
        delete adminStorage[_owner];

        size--;
    }

    function adminCount() public view returns(uint32) {
        return size;
    }

    function beActiveAdmin(address _owner) public view returns(bool) {
        return adminStorage[_owner].user != address(0) && adminStorage[_owner].status == ENABLE;
    }

    function beAdmin(address _owner) public view returns(bool) {
        return adminStorage[_owner].user != address(0);
    }

    function enableAdmin(address _owner) public {
        require(adminStorage[_owner].user != address(0), "admin does not exists");
        require(adminStorage[_owner].status == DISABLE, "duplicate enable");

        adminStorage[_owner].status = ENABLE;
    }

    function disableAdmin(address _owner) public {
        require(adminStorage[_owner].user != address(0), "admin does not exists");
        require(adminStorage[_owner].status == ENABLE, "duplicate disable");

        adminStorage[_owner].status = DISABLE;
    }

    function queryAllAdminInfo() public view returns (Admin[] memory) {
        if (size < 1) {
            return new Admin[](size);
        }
        Admin[] memory result = new Admin[](size);
        address current = adminDLLIndex[ZERO][NEXT];
        uint8 index = 0;

        while (current != ZERO) {
            result[index++] = adminStorage[current];
            current = adminDLLIndex[current][NEXT];
        }

        return result;
    }

    function queryAdminInfo(address _owner) public view returns (address, uint8) {
        require(adminStorage[_owner].user != address(0), "admin does not exist");

        Admin storage admin = adminStorage[_owner];

        return (admin.user, admin.status);
    }
}