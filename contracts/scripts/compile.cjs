const fs = require('node:fs');
const path = require('node:path');
const solc = require('solc');
const root = path.resolve(__dirname, '..');
function compile() {
  const input = { language: 'Solidity', sources: {
    'GreenBadge.sol': { content: fs.readFileSync(path.join(root, 'GreenBadge.sol'), 'utf8') }
  }, settings: { evmVersion: 'shanghai', optimizer: { enabled: true, runs: 200 },
    outputSelection: { '*': { '*': ['abi', 'evm.bytecode.object'] } } } };
  const output = JSON.parse(solc.compile(JSON.stringify(input), { import: name => {
    const file = path.resolve(root, 'node_modules', name);
    if (!file.startsWith(path.join(root, 'node_modules') + path.sep)) return { error: 'Invalid import' };
    try { return { contents: fs.readFileSync(file, 'utf8') }; } catch { return { error: 'Import missing' }; }
  } }));
  if ((output.errors || []).some(error => error.severity === 'error')) throw new Error('Contract compilation failed');
  const contract = output.contracts['GreenBadge.sol'].GreenBadge;
  return { abi: contract.abi, bytecode: '0x' + contract.evm.bytecode.object, compiler: solc.version(), evmVersion: 'shanghai' };
}
module.exports = { compile };
if (require.main === module) {
  fs.mkdirSync(path.join(root, 'artifacts'), { recursive: true });
  fs.writeFileSync(path.join(root, 'artifacts', 'GreenBadge.json'), JSON.stringify(compile(), null, 2));
  console.log('Compiled GreenBadge.json');
}
