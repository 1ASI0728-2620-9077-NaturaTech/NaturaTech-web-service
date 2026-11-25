// Read-only independent verification. Never signs or broadcasts transactions.
const fs = require('node:fs');
const path = require('node:path');
const { JsonRpcProvider, Contract, id, formatEther, parseEther } = require('ethers');
const root = path.resolve(__dirname, '../..');
async function main() {
  const values = {};
  for (const line of fs.readFileSync(path.join(root, '.env.local'), 'utf8').split(/\r?\n/)) {
    const at = line.indexOf('=');
    if (at > 0 && !line.trim().startsWith('#')) values[line.slice(0, at).trim()] = line.slice(at + 1).trim();
  }
  const rpc = process.env.SEPOLIA_RPC_URL || values.SEPOLIA_RPC_URL;
  const expectedContract = process.env.SEPOLIA_CONTRACT || values.SEPOLIA_CONTRACT;
  const record = JSON.parse(fs.readFileSync(path.join(root, '.data/sepolia-live-smoke.json'), 'utf8'));
  if (!record.broadcastRequested) throw new Error('No broadcast requested');
  const provider = new JsonRpcProvider(rpc, undefined, { batchMaxCount: 1 });
  try {
    if ((await provider.getNetwork()).chainId !== 11155111n) throw new Error('Wrong network');
    const receipt = await provider.getTransactionReceipt(record.transactionHash);
    if (!receipt) { console.log('PENDING: no mined receipt yet'); return; }
    const transaction = await provider.getTransaction(record.transactionHash);
    const block = await provider.getBlock(receipt.blockNumber);
    const contract = new Contract(expectedContract, [
      'function ownerOf(uint256) view returns(address)',
      'event Transfer(address indexed from,address indexed to,uint256 indexed tokenId)'
    ], provider);
    const tokenId = BigInt(id(record.rewardId));
    const event = receipt.logs.filter(log => log.address.toLowerCase() === expectedContract.toLowerCase())
      .map(log => { try { return contract.interface.parseLog(log); } catch { return null; } })
      .find(log => log?.name === 'Transfer' && BigInt(log.args.from) === 0n &&
        log.args.to.toLowerCase() === record.recipient && log.args.tokenId === tokenId);
    const maximumFee = transaction.gasLimit * (transaction.maxFeePerGas || transaction.gasPrice) + transaction.value;
    const fee = receipt.gasUsed * receipt.gasPrice;
    let previousFee = 0n;
    const previousPath = path.join(root, '.data/sepolia-attempt-1-failed.json');
    if (fs.existsSync(previousPath)) {
      const previous = JSON.parse(fs.readFileSync(previousPath, 'utf8'));
      const oldReceipt = await provider.getTransactionReceipt(previous.transactionHash);
      if (!oldReceipt || oldReceipt.status !== 0 || await oldReceipt.confirmations() < 12)
        throw new Error('Previous attempt not confirmed failed');
      previousFee = oldReceipt.gasUsed * oldReceipt.gasPrice;
    }
    const proof = {
      purpose: record.purpose, chainId: 11155111, transactionHash: receipt.hash,
      contract: expectedContract, tokenId: tokenId.toString(), recipient: record.recipient,
      receiptStatus: receipt.status, confirmations: await receipt.confirmations(),
      canonicalBlock: block?.hash === receipt.blockHash,
      owner: receipt.status === 1 ? await contract.ownerOf(tokenId) : null, mintEventVerified: !!event,
      maximumFeeETH: formatEther(maximumFee), actualFeeETH: formatEther(fee),
      previousAttemptFeeETH: formatEther(previousFee), totalActualFeeETH: formatEther(previousFee + fee),
      withinAuthorizedCap: maximumFee + previousFee <= parseEther('0.001') && fee + previousFee <= parseEther('0.001')
    };
    proof.verified = proof.receiptStatus === 1 && proof.confirmations >= 12 && proof.canonicalBlock &&
      proof.mintEventVerified && proof.owner?.toLowerCase() === proof.recipient && proof.withinAuthorizedCap &&
      transaction.to?.toLowerCase() === expectedContract.toLowerCase();
    fs.writeFileSync(path.join(root, '.data/sepolia-onchain-proof.json'), JSON.stringify(proof, null, 2));
    console.log(JSON.stringify(proof, null, 2));
    if (!proof.verified) process.exitCode = 2;
  } finally { provider.destroy(); }
}
main().catch(() => { console.error('Read-only verification failed; inspect local configuration and the recorded public hash.'); process.exitCode = 1; });
