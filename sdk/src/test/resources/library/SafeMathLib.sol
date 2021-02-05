// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

library SafeMathLib {
  function times(uint a, uint b) public pure returns (uint) {
    uint c = a * b;
    assert(a == 0 || c/a == b);
    return c;
  }

  function minus(uint a, uint b) public pure returns (uint) {
    assert(b <= a);
    return a - b;
  }

  function plus(uint a, uint b) public pure returns (uint) {
    uint c = a + b;
    assert(c>=a && c>=b);
    return c;
  }
}