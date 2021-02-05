// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

interface Calculator {
    function getResult() external view returns(uint);
}

contract InterfaceTest is Calculator {

    function getResult() external override(Calculator)view returns(uint){
        uint a = 1;
        uint b = 2;
        uint result = a + b;
        return result;
    }
}
