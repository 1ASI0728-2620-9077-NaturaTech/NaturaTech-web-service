// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;
import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";

/** One non-transferable badge per opaque achievement. No personal information on chain. */
contract GreenBadge is ERC721, AccessControl, Pausable {
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    mapping(bytes32 => bool) public issued;
    constructor(address admin, address minter) ERC721("PlantSync Eco Badge", "ECO") {
        require(admin != address(0) && minter != address(0), "zero address");
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(MINTER_ROLE, minter);
    }
    function mint(address recipient, bytes32 achievement) external onlyRole(MINTER_ROLE) whenNotPaused {
        require(!issued[achievement], "already issued");
        issued[achievement] = true;
        _safeMint(recipient, uint256(achievement));
    }
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) { _pause(); }
    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) { _unpause(); }
    function _update(address to, uint256 tokenId, address auth) internal override returns (address) {
        require(_ownerOf(tokenId) == address(0), "badge cannot be transferred");
        return super._update(to, tokenId, auth);
    }
    function supportsInterface(bytes4 id) public view override(ERC721, AccessControl) returns (bool) {
        return super.supportsInterface(id);
    }
}

