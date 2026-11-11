"""Prepare local simulator secrets without printing them or replacing existing credentials."""
import argparse
import hashlib
import json
import secrets
from pathlib import Path


def prepare(path, node_code):
    original = path.read_text(encoding='utf-8') if path.exists() else ''
    values = {}
    for line in original.splitlines():
        key, separator, value = line.partition('=')
        if separator and not key.strip().startswith('#'):
            values[key.strip()] = value.strip()
    edge = values.get('EDGE_API_KEY') or secrets.token_urlsafe(48)
    device = values.get('IOT_NODE_KEY') or secrets.token_urlsafe(48)
    if len(device) < 32:
        raise ValueError('Existing IOT_NODE_KEY is too short; correct it locally')
    hashes = json.loads(values.get('IOT_NODE_KEY_HASHES') or '{}')
    digest = hashlib.sha256(device.encode()).hexdigest()
    if node_code in hashes and hashes[node_code] != digest:
        raise ValueError('Existing node hash differs; refusing to replace credentials')
    hashes[node_code] = digest
    nodes = {value.strip() for value in values.get('IOT_APPROVED_NODES', '').split(',') if value.strip()}
    nodes.add(node_code)
    updates = {'EDGE_API_KEY': edge, 'IOT_NODE_KEY': device, 'IOT_NODE_KEY_HASHES': json.dumps(hashes, separators=(',', ':')),
               'IOT_APPROVED_NODES': ','.join(sorted(nodes)), 'IOT_ENVIRONMENT': 'SIMULATED'}
    lines = [line for line in original.splitlines() if line.partition('=')[0].strip() not in updates]
    path.write_text('\n'.join(lines + [key + '=' + value for key, value in updates.items()]) + '\n', encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--node-code', required=True)
    args = parser.parse_args()
    if not args.node_code or any(c in args.node_code for c in '\r\n,='):
        parser.error('Invalid node code')
    prepare(Path('.env.local'), args.node_code)
    print('Simulator configured in .env.local; no secrets printed. Restart the local backend.')
