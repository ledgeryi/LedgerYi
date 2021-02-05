// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

import './ERC20Lib.sol';

contract StandardToken {
   using ERC20Lib for ERC20Lib.TokenStorage;

   ERC20Lib.TokenStorage token;

   string public name = "SimpleToken";
   string public symbol = "SIM";
   uint public decimals = 18;
   uint public INITIAL_SUPPLY = 10000;

   function StandardTokenInit() public {
     token.init(INITIAL_SUPPLY);
   }

   function totalSupply() public view returns (uint) {
     return token.totalSupply;
   }

   function balanceOf(address who) public view returns (uint) {
     return token.balanceOf(who);
   }

   function allowance(address owner, address spender) public view returns (uint) {
     return token.allowance(owner, spender);
   }

   function transfer(address to, uint value) public returns (bool ok) {
     return token.transfer(to, value);
   }

   function transferFrom(address from, address to, uint value) public returns (bool ok) {
     return token.transferFrom(from, to, value);
   }

   function approve(address spender, uint value) public returns (bool ok) {
     return token.approve(spender, value);
   }

   event Transfer(address indexed from, address indexed to, uint value);
   event Approval(address indexed owner, address indexed spender, uint value);
}
