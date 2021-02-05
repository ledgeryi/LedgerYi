// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.9;

library Math {

    struct Balance {
        uint256 _balance;
    }

    function add(Balance storage self, uint256 amount) public {
        uint256 newBalance = self._balance + amount;
        require(newBalance >= self._balance, "addition overflow");
        self._balance = newBalance;
    }
}