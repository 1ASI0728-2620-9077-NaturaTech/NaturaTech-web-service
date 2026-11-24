const { test } = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const { JsonRpcProvider, ContractFactory, id, ZeroAddress } = require('ethers');
const { compile } = require('../scripts/compile.cjs');

test('GreenBadge: real EVM deployment, roles, mint, duplicates, pause and transfers', async () => {
  const name = 'plantsync-contract-' + randomUUID().slice(0, 8);
  const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  docker('run', '--detach', '--name', name, '--publish', '127.0.0.1::8545', '--entrypoint', 'anvil', 'ghcr.io/foundry-rs/foundry@sha256:043752653d5be351c71709091b3db97c4421c907eb40ea294195e7f532aadf46', '--host', '0.0.0.0');
  const provider = new JsonRpcProvider('http://' + docker('port', name, '8545/tcp'), undefined, { cacheTimeout: -1 });
  provider.pollingInterval = 20;
  try {
    for (let attempt = 0; attempt < 30; attempt++) {
      try { await provider.getBlockNumber(); break; }
      catch { await new Promise(resolve => setTimeout(resolve, 100)); }
    }
    const admin = await provider.getSigner(0), minter = await provider.getSigner(1);
    const recipient = await provider.getSigner(2), outsider = await provider.getSigner(3);
    const artifact = compile();
    const factory = new ContractFactory(artifact.abi, artifact.bytecode, admin);
    await assert.rejects(factory.deploy(ZeroAddress, await minter.getAddress()));
    const contract = await factory.deploy(await admin.getAddress(), await minter.getAddress());
    await contract.waitForDeployment();
    const achievement = id('integration-achievement');
    await assert.rejects(contract.connect(outsider).mint(await recipient.getAddress(), achievement));
    await (await contract.connect(minter).mint(await recipient.getAddress(), achievement)).wait();
    assert.equal(await contract.ownerOf(BigInt(achievement)), await recipient.getAddress());
    assert.equal(await contract.issued(achievement), true);
    await assert.rejects(async () => (await contract.connect(minter).mint(await recipient.getAddress(), achievement)).wait());
    await assert.rejects(contract.connect(recipient).transferFrom(await recipient.getAddress(), await outsider.getAddress(), BigInt(achievement)));
    await assert.rejects(contract.connect(outsider).pause());
    await (await contract.pause()).wait();
    await assert.rejects(contract.connect(minter).mint(await recipient.getAddress(), id('paused')));
    await (await contract.unpause()).wait();
    await (await contract.connect(minter).mint(await recipient.getAddress(), id('unpaused'))).wait();
    assert.equal(await contract.balanceOf(await recipient.getAddress()), 2n);
    await (await contract.revokeRole(await contract.MINTER_ROLE(), await minter.getAddress())).wait();
    await assert.rejects(contract.connect(minter).mint(await recipient.getAddress(), id('revoked')));
  } finally { provider.destroy(); docker('rm', '--force', '--volumes', name); }
});
