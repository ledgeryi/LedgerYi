// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;
import "./erc20.sol";

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