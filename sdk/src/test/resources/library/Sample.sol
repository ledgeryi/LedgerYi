// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;
import {SafeMath} from "./SafeMath.sol";

contract Sample {

    using SafeMath for uint256;

    uint256 public total;

    function safeAdd(uint256 a, uint256 b) public pure returns (uint256){
        return a.add(b);
    }

    function sum(uint256 a, uint256 b) public returns (bool){
        total = a.add(b);
        return true;
    }
}