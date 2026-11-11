"""PlantSync REST edge simulator. No physical devices or accelerated reward history."""
import argparse
import json
import os
import sqlite3
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import quote, urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None  # Never forward edge credentials to a redirected destination.


class Api:
    def __init__(self, base, node):
        parsed = urlparse(base)
        if parsed.scheme != 'https' and not (parsed.scheme == 'http' and parsed.hostname in ('localhost', '127.0.0.1')):
            raise ValueError('Use HTTPS or a localhost URL')
        if parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ValueError('Invalid API URL')
        self.base, self.node = base.rstrip('/'), quote(node, safe='')
        self.edge, self.secret = os.environ['EDGE_API_KEY'], os.environ['IOT_NODE_KEY']
        if not self.edge or len(self.secret) < 32:
            raise ValueError('Configure EDGE_API_KEY and IOT_NODE_KEY (at least 32 characters)')
        self.opener = build_opener(NoRedirect())

    def call(self, path, body=None):
        data = None if body is None else json.dumps(body).encode()
        request = Request(self.base + path, data=data, headers={
            'Content-Type': 'application/json', 'X-Edge-Api-Key': self.edge, 'X-Node-Key': self.secret})
        with self.opener.open(request, timeout=10) as response:
            return json.load(response)

    def commands(self):
        return self.call(f'/iot/edge/nodes/{self.node}/agent-commands')

    def receipt(self, identifier, status):
        return self.call(f'/iot/edge/nodes/{self.node}/agent-commands/{quote(identifier, safe="")}/receipt', {'status': status})


class Simulator:
    def __init__(self, api, journal, clock=time.time):
        self.api, self.clock = api, clock
        self.db = sqlite3.connect(journal)
        self.db.execute('CREATE TABLE IF NOT EXISTS commands (id TEXT PRIMARY KEY, actuator TEXT, deadline REAL, state TEXT, reported INTEGER DEFAULT 0)')
        # A restart cannot establish whether interrupted simulated execution completed.
        self.db.execute("UPDATE commands SET state='FAILED', reported=0 WHERE state='RUNNING'")
        self.db.commit()

    def accept(self, command):
        identifier = command.get('commandId')
        actuator, duration = command.get('actuator'), command.get('durationSeconds')
        limits = {'GROW_LIGHT': 300, 'WATER_PUMP': 10}
        if not isinstance(identifier, str) or not identifier or len(identifier) > 100:
            return False
        if actuator not in limits or type(duration) is not int or not 1 <= duration <= limits[actuator]:
            return False
        try:
            expires = datetime.fromisoformat(command['expiresAt'].replace('Z', '+00:00'))
            if expires.tzinfo is None or expires.timestamp() <= self.clock():
                return False
        except (KeyError, ValueError, TypeError, AttributeError):
            return False
        result = self.db.execute('INSERT OR IGNORE INTO commands(id,actuator,deadline,state) VALUES(?,?,?,?)',
                                 (identifier, actuator, self.clock() + duration, 'RUNNING'))
        self.db.commit()  # Persist deduplication before acknowledging simulated activation.
        if result.rowcount:
            print(f'[SIMULATED] {actuator} ON, command={identifier}', flush=True)
            return True
        return False

    def tick(self):
        self.db.execute("UPDATE commands SET state='EXECUTED', reported=0 WHERE state='RUNNING' AND deadline<=?", (self.clock(),))
        self.db.commit()  # Local cutoff precedes any network request.
        for identifier, state in self.db.execute('SELECT id,state FROM commands WHERE reported=0').fetchall():
            try:
                self.api.receipt(identifier, 'ACKNOWLEDGED' if state == 'RUNNING' else state)
                self.db.execute('UPDATE commands SET reported=1 WHERE id=? AND state=?', (identifier, state))
                self.db.commit()
            except Exception as error:
                print(f'[SIMULATED] Receipt pending ({type(error).__name__})', flush=True)

    def reading(self, node_id):
        active = {row[0] for row in self.db.execute("SELECT actuator FROM commands WHERE state='RUNNING' AND deadline>?", (self.clock(),))}
        return {'nodeId': node_id, 'lightPercent': 70 if 'GROW_LIGHT' in active else 40,
                'humidityPercent': 60 if 'WATER_PUMP' in active else 45, 'gasPercent': 10, 'airTemperature': 23.0}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8080/api/v1')
    parser.add_argument('--node-code', required=True)
    parser.add_argument('--node-id', required=True, type=int)
    parser.add_argument('--journal', default='.data/iot-simulator.sqlite')
    parser.add_argument('--env-file', default='.env.local')
    args = parser.parse_args()
    # Same simple KEY=value format as Spring local properties; never execute file contents.
    config = Path(args.env_file)
    if config.exists():
        for line in config.read_text(encoding='utf-8').splitlines():
            key, separator, value = line.partition('=')
            if separator and key.strip() in ('EDGE_API_KEY', 'IOT_NODE_KEY'):
                os.environ.setdefault(key.strip(), value.strip())
    if args.node_id <= 0:
        parser.error('node-id must be positive')
    path = Path(args.journal)
    path.parent.mkdir(parents=True, exist_ok=True)
    api = Api(args.base_url, args.node_code)
    simulator = Simulator(api, str(path))
    print('[SIMULATED] No hardware connected. One process per node; retain the journal between restarts.', flush=True)
    next_reading = 0
    try:
        while True:
            simulator.tick()
            try:
                if time.monotonic() >= next_reading:
                    api.call('/iot/edge/sensor-readings/batch', [simulator.reading(args.node_id)])
                    next_reading = time.monotonic() + 60
                for command in api.commands():
                    simulator.accept(command)
            except Exception as error:
                # Do not print response bodies, URLs or credentials.
                print(f'[SIMULATED] Backend unavailable ({type(error).__name__}); will retry.', flush=True)
            time.sleep(2)
    except KeyboardInterrupt:
        print('[SIMULATED] Stopped. Interrupted commands fail on next startup.', flush=True)
    finally:
        simulator.db.close()


if __name__ == '__main__':
    main()
