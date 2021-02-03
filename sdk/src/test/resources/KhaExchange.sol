// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;


contract KhaExchange {

    address private toAddress;

    constructor(address _toAddress) public {
        toAddress = _toAddress;
    }

    function modifyToAddress(address _toAddress) public {
        toAddress = _toAddress;
    }

    function depositEth(address otherContract, uint256 amount) public payable returns(bool) {
        require(amount > 0);
        Erc20 erc20 = Erc20(otherContract);
        require(erc20.transferFrom(msg.sender, toAddress, amount));
        return true;
    }

    function balanceOfOwner(address otherContract, address owner) public view returns(uint) {
        Erc20 erc20 = Erc20(otherContract);
        return erc20.balanceOf(owner);
    }
}

contract Erc20 {

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

    function balanceOf(address tokenOwner) public view returns (uint256) {
        return balances[tokenOwner];
    }

    function burn(address account, uint256 amount) public {
        require(amount <= balances[account]);
        balances[account] = sub(balances[account], amount);
        totalSupply = sub(totalSupply, amount);
        emit Burn(account, amount);
    }

    function mint(address account, uint256 amount) public {
        balances[account] = add(balances[account], amount);
        totalSupply = add(totalSupply, amount);
        emit Mint(account, amount);
    }

    function transfer(address receiver, uint numTokens) public returns (bool) {
        require(numTokens <= balances[msg.sender]);
        balances[msg.sender] = sub(balances[msg.sender] , numTokens);
        balances[receiver] = add(balances[receiver], numTokens);
        emit Transfer(msg.sender, receiver, numTokens);
        return true;
    }

    function transferFrom(address from, address receiver, uint numTokens) public returns (bool) {
        require(numTokens <= balances[from]);
        balances[from] = sub(balances[from] , numTokens);
        balances[receiver] = add(balances[receiver], numTokens);
        emit Transfer(msg.sender, receiver, numTokens);
        return true;
    }

    function sub(uint256 a, uint256 b) internal pure returns (uint256) {
        assert(b <= a);
        return a - b;
    }

    function add(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        assert(c >= a);
        return c;
    }

}

