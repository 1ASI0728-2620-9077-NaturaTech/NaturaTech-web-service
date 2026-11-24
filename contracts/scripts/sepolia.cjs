// Only a dedicated testnet minter key is used. Never import the user's seed phrase.
const fs = require('node:fs');
const path = require('node:path');
const { Wallet, JsonRpcProvider, ContractFactory, Contract, formatEther, getAddress } = require('ethers');
const { compile } = require('./compile.cjs');
const root = path.resolve(__dirname, '../..');
const envPath = path.join(root, '.env.local');
const recordPath = path.join(root, '.data', 'sepolia-deployment.json');
function readConfig() {
  const values = {};
  if (fs.existsSync(envPath)) for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const index = line.indexOf('=');
    if (index > 0 && !line.trim().startsWith('#')) values[line.slice(0, index).trim()] = line.slice(index + 1).trim();
  }
  return { ...values, ...process.env };
}
function update(values) {
  const old = fs.existsSync(envPath) ? fs.readFileSync(envPath, 'utf8') : '';
  const kept = old.split(/\r?\n/).filter(line => !Object.hasOwn(values, line.split('=')[0].trim()));
  fs.writeFileSync(envPath, kept.join('\n').trimEnd() + '\n' + Object.entries(values).map(([k,v]) => k + '=' + v).join('\n') + '\n', { mode: 0o600 });
}
async function main() {
  const command = process.argv[2] || 'check';
  let config = readConfig();
  if (command === 'init') {
    const changes = {};
    if (!config.SEPOLIA_MINTER_KEY) changes.SEPOLIA_MINTER_KEY = Wallet.createRandom().privateKey;
    if (!config.SEPOLIA_RPC_URL) changes.SEPOLIA_RPC_URL = 'https://ethereum-sepolia-rpc.publicnode.com';
    if (Object.keys(changes).length) update(changes);
    config = readConfig();
  }
  if (!config.SEPOLIA_RPC_URL || !config.SEPOLIA_MINTER_KEY) throw new Error('Run npm run sepolia:init first');
  const provider = new JsonRpcProvider(config.SEPOLIA_RPC_URL, undefined, { batchMaxCount: 1 });
  try {
    const wallet = new Wallet(config.SEPOLIA_MINTER_KEY, provider);
    if ((await provider.getNetwork()).chainId !== 11155111n) throw new Error('RPC must be Ethereum Sepolia');
    const balance = await provider.getBalance(wallet.address);
    console.log(JSON.stringify({ chainId: 11155111, minter: wallet.address, balanceETH: formatEther(balance), contract: config.SEPOLIA_CONTRACT || null }, null, 2));
    if (command === 'recover') {
      const record = JSON.parse(fs.readFileSync(recordPath, 'utf8'));
      if (record.chainId !== 11155111 || record.minter !== wallet.address) throw new Error('Deployment record mismatch');
      const receipt = await provider.getTransactionReceipt(record.transactionHash);
      if (!receipt || receipt.status !== 1 || receipt.contractAddress?.toLowerCase() !== record.contract.toLowerCase())
        throw new Error('Deployment is pending, failed or inconsistent');
      const deployed = new Contract(record.contract, compile().abi, provider);
      if (!(await deployed.hasRole(await deployed.MINTER_ROLE(), wallet.address))) throw new Error('Minter role missing');
      if (!(await deployed.hasRole(await deployed.DEFAULT_ADMIN_ROLE(), record.admin))) throw new Error('Admin role missing');
      if (await deployed.paused()) throw new Error('Contract is paused');
      update({ SEPOLIA_CONTRACT: record.contract });
      console.log('Recovered confirmed contract: ' + record.contract);
      return;
    }
    if (command === 'init' || command === 'check') {
      if (config.SEPOLIA_CONTRACT) {
        if (await provider.getCode(config.SEPOLIA_CONTRACT) === '0x') throw new Error('Configured contract has no code');
        const contract = new Contract(config.SEPOLIA_CONTRACT, compile().abi, provider);
        if (!(await contract.hasRole(await contract.MINTER_ROLE(), wallet.address))) throw new Error('Minter role missing');
        console.log('PASS: contract code and minter role');
      }
      return;
    }
    if (command !== 'deploy') throw new Error('Expected init, check or deploy');
    const adminArg = process.argv.find(arg => arg.startsWith('--admin='));
    if (!adminArg) throw new Error('Specify --admin=PUBLIC_RABBY_ADDRESS');
    const admin = getAddress(adminArg.slice(8).toLowerCase());
    if (config.SEPOLIA_CONTRACT) throw new Error('Contract already configured; refusing duplicate deployment');
    if (fs.existsSync(recordPath)) throw new Error('Deployment record exists; reconcile its transaction before deploying again');
    const artifact = compile();
    const factory = new ContractFactory(artifact.abi, artifact.bytecode, wallet);
    const transaction = await factory.getDeployTransaction(admin, wallet.address);
    const gas = await provider.estimateGas({ ...transaction, from: wallet.address });
    const fees = await provider.getFeeData();
    const price = fees.maxFeePerGas || fees.gasPrice;
    if (!price) throw new Error('Cannot estimate fees');
    const budget = gas * 120n / 100n * price;
    console.log('Estimated maximum deployment budget ETH: ' + formatEther(budget));
    if (budget > 10000000000000000n) throw new Error('Deployment exceeds 0.01 test ETH budget');
    if (balance < budget) throw new Error('Fund the dedicated minter with Sepolia test ETH');
    if (!process.argv.includes('--broadcast')) { console.log('Dry run only. Add --broadcast to deploy.'); return; }
    const contract = await factory.deploy(admin, wallet.address, { gasLimit: gas * 120n / 100n });
    const record = { chainId: 11155111, contract: await contract.getAddress(), admin, minter: wallet.address,
      transactionHash: contract.deploymentTransaction().hash, compiler: artifact.compiler, evmVersion: artifact.evmVersion };
    fs.mkdirSync(path.dirname(recordPath), { recursive: true });
    fs.writeFileSync(recordPath, JSON.stringify(record, null, 2));
    console.log('Deployment transaction: ' + record.transactionHash);
    await contract.deploymentTransaction().wait(2, 180000);
    update({ SEPOLIA_CONTRACT: record.contract });
    console.log('Deployed contract: ' + record.contract);
    console.log('REWARDS_ENABLED remains unchanged; configure rules and verify the full flow before enabling.');
  } finally { provider.destroy(); }
}
main().catch(() => { console.error('Sepolia operation failed. Check RPC, funding, configuration and any deployment record. No secrets printed.'); process.exitCode = 1; });
