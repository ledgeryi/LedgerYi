// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;
import "./Utils.sol";

contract StoreWithSerialize{
    string internal symbol;

    constructor(string memory symbol_) public {
        symbol = symbol_;
    }

    function setSymbol(string memory _symbol_) public {
        symbol = _symbol_;
    }

    function getSymbol() public  view returns (string memory _symbol_){
        _symbol_ = symbol;
    }
}