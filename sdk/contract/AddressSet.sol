// SPDX-License-Identifier: GPL-3.0

pragma solidity ^0.6.9;

library AddressSet {

    struct WhiteList {
        Set accessList;
        address owner;
        bool status;
    }

    struct Set {
        // Storage of set values
        address[] _values;

        // Position of the value in the `values` array, plus 1 because index 0
        // means a value is not in the set.
        mapping (bytes32 => uint256) _indexes;

    }

    function setOwner(WhiteList storage whiteList, address _owner) internal {
        whiteList.owner = _owner;
    }

    function setStatus(WhiteList storage whiteList, bool _status) internal {
        whiteList.status = _status;
    }

    function addUser(WhiteList storage whiteList, address _user) internal returns (bool) {
        return add(whiteList.accessList, _user);
    }

    function contains(WhiteList storage whiteList, address _user) internal view returns (bool) {
        return contains(whiteList.accessList, _user);
    }

    function remove(WhiteList storage whiteList, address _user) internal returns (bool) {
        return remove(whiteList.accessList, _user);
    }

    function length(WhiteList storage whiteList) internal view returns (uint256) {
        return length(whiteList.accessList);
    }

    function at(WhiteList storage whiteList, uint256 _index) internal view returns (address) {
        return at(whiteList.accessList, _index);
    }


    /**
     * @dev Add a value to a set. O(1).
     *
     * Returns true if the value was added to the set, that is if it was not
     * already present.
     */
    function add(Set storage set, address value) internal returns (bool) {
        if (!contains(set, value)) {
            set._values.push(value);
            // The value is stored at length-1, but we add 1 to all indexes and use 0 as a sentinel value
            bytes32 _key = keccak256(abi.encodePacked(value));
            set._indexes[_key] = set._values.length;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @dev Removes a value from a set. O(1).
     *
     * Returns true if the value was removed from the set, that is if it was
     * present.
     */
    function remove(Set storage set, address value) internal returns (bool) {
        // We read and store the value's index to prevent multiple reads from the same storage slot
        uint256 valueIndex = set._indexes[keccak256(abi.encodePacked(value))];

        if (valueIndex != 0) { // Equivalent to contains(set, value)
            // To delete an element from the _values array in O(1), we swap the element to delete with the last one in
            // the array, and then remove the last element (sometimes called as 'swap and pop').
            // This modifies the order of the array, as noted in {at}.

            uint256 toDeleteIndex = valueIndex - 1;
            uint256 lastIndex = set._values.length - 1;

            // When the value to delete is the last one, the swap operation is unnecessary. However, since this occurs
            // so rarely, we still do the swap anyway to avoid the gas cost of adding an 'if' statement.

            address lastvalue = set._values[lastIndex];

            // Move the last value to the index where the value to delete is
            set._values[toDeleteIndex] = lastvalue;
            // Update the index for the moved value
            set._indexes[keccak256(abi.encodePacked(lastvalue))] = toDeleteIndex + 1; // All indexes are 1-based

            // Delete the slot where the moved value was stored
            set._values.pop();

            // Delete the index for the deleted slot
            delete set._indexes[keccak256(abi.encodePacked(value))];

            return true;
        } else {
            return false;
        }
    }

    /**
     * @dev Returns true if the value is in the set. O(1).
     */
    function contains(Set storage set, address value) internal view returns (bool) {
        return set._indexes[keccak256(abi.encodePacked(value))] != 0;
    }

    /**
     * @dev Returns the number of values on the set. O(1).
     */
    function length(Set storage set) internal view returns (uint256) {
        return set._values.length;
    }

    /**
     * @dev Returns the value stored at position `index` in the set. O(1).
     *
     * Note that there are no guarantees on the ordering of values inside the
     * array, and it may change when more values are added or removed.
     *
     * Requirements:
     *
     * - `index` must be strictly less than {length}.
     */
    function at(Set storage set, uint256 index) internal view returns (address) {
        require(set._values.length > index, "EnumerableSet: index out of bounds");
        return set._values[index];
    }
}