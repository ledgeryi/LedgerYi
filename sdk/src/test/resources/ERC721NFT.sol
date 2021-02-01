// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.6.8;

contract ERC721NFT {

    string internal nftName;
    string internal nftSymbol;
    address internal creator;

    mapping(uint256 => bool) internal burned;
    mapping(uint256 => address) internal owners;
    mapping(address => uint256) internal balances;

    uint256 internal maxId;
    uint[] internal tokenIndexes;
    mapping(uint => uint) internal indexTokens;
    mapping(uint => uint) internal tokenTokenIndexes;
    mapping(address => uint[]) internal ownerTokenIndexes;

    constructor(string memory name_, string memory symbol_, uint _initialSupply) public {
        require(_initialSupply <= 10000);
        creator = msg.sender;
        nftName = name_;
        nftSymbol = symbol_;
        maxId = _initialSupply;
        balances[msg.sender] = _initialSupply;
        for(uint i = 0; i < _initialSupply; i++){
            tokenTokenIndexes[i+1] = i;
            ownerTokenIndexes[creator].push(i+1);
            tokenIndexes.push(i+1);
            indexTokens[i+1] = i;
        }
    }

    function isValidToken(uint256 _tokenId) public view returns (bool){
        return _tokenId != 0 && _tokenId <= maxId && !burned[_tokenId];
    }

    function balanceOf(address _owner) public view returns (uint256){
        return balances[_owner];
    }

    function ownerOf(uint256 _tokenId) public view returns (address){
        require(isValidToken(_tokenId));
        if(owners[_tokenId] != address(0) ){
            return owners[_tokenId];
        }else{
            return creator;
        }
    }

    function totalSupply() public view returns (uint256){
        return tokenIndexes.length;
    }

    function tokenByIndex(uint256 _index) public view returns(uint256){
        require(_index < tokenIndexes.length);
        return tokenIndexes[_index];
    }

    function tokenOfOwnerByIndex(address _owner, uint256 _index) public view returns (uint256){
        require(_index < balances[_owner]);
        return ownerTokenIndexes[_owner][_index];
    }

    function transferFrom(address _from, address _to, uint256 _tokenId) public {
        address owner = ownerOf(_tokenId);
        require (owner == msg.sender);
        require(owner == _from);
        require(_to != address(0));
        require(isValidToken(_tokenId));
        owners[_tokenId] = _to;
        balances[_from]--;
        balances[_to]++;
        uint oldIndex = tokenTokenIndexes[_tokenId];
        if(oldIndex != ownerTokenIndexes[_from].length - 1){
            ownerTokenIndexes[_from][oldIndex] = ownerTokenIndexes[_from][ownerTokenIndexes[_from].length - 1];
            tokenTokenIndexes[ownerTokenIndexes[_from][oldIndex]] = oldIndex;
        }
        ownerTokenIndexes[_from].pop();
        tokenTokenIndexes[_tokenId] = ownerTokenIndexes[_to].length;
        ownerTokenIndexes[_to].push(_tokenId);
    }

    function issueTokens(uint256 _extraTokens) public{
        require(msg.sender == creator);
        balances[msg.sender] = balances[msg.sender] + (_extraTokens);
        uint thisId;
        for(uint i = 0; i < _extraTokens; i++){
            thisId = maxId + i + 1;
            tokenTokenIndexes[thisId] = ownerTokenIndexes[creator].length;
            ownerTokenIndexes[creator].push(thisId);
            indexTokens[thisId] = tokenIndexes.length;
            tokenIndexes.push(thisId);
        }
        maxId = maxId + _extraTokens;
    }

    function burnToken(uint256 _tokenId) public{
        address owner = ownerOf(_tokenId);
        require(owner == msg.sender);
        burned[_tokenId] = true;
        balances[owner]--;
        uint oldIndex = tokenTokenIndexes[_tokenId];
        if(oldIndex != ownerTokenIndexes[owner].length - 1){
            ownerTokenIndexes[owner][oldIndex] = ownerTokenIndexes[owner][ownerTokenIndexes[owner].length - 1];
            tokenTokenIndexes[ownerTokenIndexes[owner][oldIndex]] = oldIndex;
        }
        ownerTokenIndexes[owner].pop();
        delete tokenTokenIndexes[_tokenId];
        oldIndex = indexTokens[_tokenId];
        if(oldIndex != tokenIndexes.length - 1){
            tokenIndexes[oldIndex] = tokenIndexes[tokenIndexes.length - 1];
            indexTokens[ tokenIndexes[oldIndex] ] = oldIndex;
        }
        tokenIndexes.pop();
        delete indexTokens[_tokenId];
    }

    function name() external view returns (string memory _name) {
        _name = nftName;
    }

    function symbol() external view returns (string memory _symbol){
        _symbol = nftSymbol;
    }
}