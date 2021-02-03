// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;
import "./Utils.sol";

contract Erc20 is Utils{

    string public name;
    string public symbol;
    uint256 public totalSupply;

    event Transfer(address indexed from, address indexed to, uint tokens);
    event Burn(address indexed account, uint256 amount);
    event Mint(address indexed account, uint256 amount);

    mapping(address => uint256) balances;

    constructor(string memory name_, string memory symbol_, uint256 totalSupply_) public {
        balances[msg.sender] = totalSupply_;
        name = name_;
        symbol = symbol_;
        totalSupply = totalSupply_;
    }

    function balanceOf(address tokenOwner) public validAddress(tokenOwner) view returns (uint) {
        return balances[tokenOwner];
    }

    function burn(address account, uint256 amount) public validAddress(account) {
        require(amount <= balances[account]);
        balances[account] = safeSub(balances[account], amount);
        totalSupply = safeSub(totalSupply, amount);
        emit Burn(account, amount);
    }

    function mint(address account, uint256 amount) public validAddress(account) {
        balances[account] = safeAdd(balances[account], amount);
        totalSupply = safeAdd(totalSupply, amount);
        emit Mint(account, amount);
    }

    function transfer(address receiver, uint numTokens) public validAddress(receiver) returns (bool) {
        require(numTokens <= balances[msg.sender]);
        balances[msg.sender] = safeSub(balances[msg.sender] , numTokens);
        balances[receiver] = safeAdd(balances[receiver], numTokens);
        emit Transfer(msg.sender, receiver, numTokens);
        return true;
    }

    function transferFrom(address from, address receiver, uint numTokens) public validAddresses(from, receiver) returns (bool) {
        require(numTokens <= balances[from]);
        balances[from] = safeSub(balances[from] , numTokens);
        balances[receiver] = safeAdd(balances[receiver], numTokens);
        emit Transfer(from, receiver, numTokens);
        return true;
    }
}