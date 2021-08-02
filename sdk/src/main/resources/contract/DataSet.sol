// SPDX-License-Identifier: GPL-3.0
pragma experimental ABIEncoderV2;

pragma solidity ^0.6.9;

library DataSet {

    struct Set {
        // Storage of set keys
        string[] _keys;

        // Position of the value in the `values` array, plus 1 because index 0
        // means a value is not in the set.
        mapping (bytes32 => uint256) _indexes;
    }

    struct Data {
        mapping (bytes32 => string) _data;
    }

    struct DataList {
        Set _keyInfo;
        Data[] _datas;
    }

    function addData(DataList storage dataList, string[] memory _data) public returns (uint256) {
        require(_data.length == dataList._keyInfo._keys.length, "The data length is inconsistent");
        dataList._datas.push(Data());
        uint256 size = dataList._datas.length;
        addData(dataList._datas[size - 1], dataList._keyInfo._keys, _data);
        return size - 1;
    }

    function addData(Data storage data, string[] memory _keys, string[] memory _values) private {
        uint256 paramLength = _values.length;
        for (uint i = 0; i < paramLength; i++){
            data._data[keccak256(abi.encodePacked(_keys[i]))] = _values[i];
        }
    }

    function getData(DataList storage dataList, uint256 _index) public view returns (string[] memory, string[] memory) {
        require(_index < dataList._datas.length, "Data index out of bounds");
        uint256 size = dataList._keyInfo._keys.length;
        string[] memory _dataList = new string[](size);
        for (uint256 i = 0; i < size; i++) {
            _dataList[i] = dataList._datas[_index]._data[keccak256(abi.encodePacked(dataList._keyInfo._keys[i]))];
        }
        return (dataList._keyInfo._keys, _dataList);
    }

    function addKey(DataList storage dataList, string memory value) public returns (bool) {
        return add(dataList._keyInfo, value);
    }

    function getKey(DataList storage dataList) public view returns (string[] memory) {
        return dataList._keyInfo._keys;
    }

    function length(DataList storage dataList) public view returns (uint256) {
        return dataList._datas.length;
    }


    /**
     * @dev Add a value to a set. O(1).
     *
     * Returns true if the value was added to the set, that is if it was not
     * already present.
     */
    function add(Set storage set, string memory value) private returns (bool) {
        if (!contains(set, value)) {
            set._keys.push(value);
            // The value is stored at length-1, but we add 1 to all indexes and use 0 as a sentinel value
            bytes32 _key = keccak256(abi.encodePacked(value));
            set._indexes[_key] = set._keys.length;
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
    function remove(Set storage set, string memory value) private returns (bool) {
        // We read and store the value's index to prevent multiple reads from the same storage slot
        uint256 valueIndex = set._indexes[keccak256(abi.encodePacked(value))];

        if (valueIndex != 0) { // Equivalent to contains(set, value)
            // To delete an element from the _keys array in O(1), we swap the element to delete with the last one in
            // the array, and then remove the last element (sometimes called as 'swap and pop').
            // This modifies the order of the array, as noted in {at}.

            uint256 toDeleteIndex = valueIndex - 1;
            uint256 lastIndex = set._keys.length - 1;

            // When the value to delete is the last one, the swap operation is unnecessary. However, since this occurs
            // so rarely, we still do the swap anyway to avoid the gas cost of adding an 'if' statement.

            string storage lastvalue = set._keys[lastIndex];

            // Move the last value to the index where the value to delete is
            set._keys[toDeleteIndex] = lastvalue;
            // Update the index for the moved value
            set._indexes[keccak256(abi.encodePacked(lastvalue))] = toDeleteIndex + 1; // All indexes are 1-based

            // Delete the slot where the moved value was stored
            set._keys.pop();

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
    function contains(Set storage set, string memory value) private view returns (bool) {
        return set._indexes[keccak256(abi.encodePacked(value))] != 0;
    }

    /**
     * @dev Returns the number of values on the set. O(1).
     */
    function length(Set storage set) private view returns (uint256) {
        return set._keys.length;
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
    function at(Set storage set, uint256 index) private view returns (string memory) {
        require(set._keys.length > index, "EnumerableSet: index out of bounds");
        return set._keys[index];
    }
}