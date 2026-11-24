"""Explicit live Gemini + simulated actuator smoke test in a disposable Docker API.

Uses local credentials without printing them. Makes billable provider requests.
The image fixture is deliberately blank: this checks the image integration, not
botanical accuracy. No production data or physical actuator is used.
"""
import hashlib
import json
import os
from pathlib import Path
import secrets
import struct
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid
import zlib

ROOT = Path(__file__).resolve().parents[1]


def docker(*args):
    result = subprocess.run(['docker', *args], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError('Docker operation failed (details omitted)')
    return result.stdout.strip()


def main():
    values = {}
    for line in (ROOT / '.env.local').read_text(encoding='utf-8-sig').splitlines():
        key, sep, value = line.partition('=')
        if sep and not key.strip().startswith('#'):
            values[key.strip()] = value.strip()
    # Match Spring: process environment takes precedence over imported properties.
    values.update(os.environ)
    if not values.get('GEMINI_API_KEY') or not values.get('GEMINI_MODEL'):
        raise RuntimeError('Local Gemini configuration missing')
    node_secret, edge = secrets.token_urlsafe(48), secrets.token_urlsafe(48)
    name = 'plantsync-ai-check-' + uuid.uuid4().hex[:12]
    config = {
        'SPRING_PROFILES_ACTIVE': 'local', 'JWT_SECRET': secrets.token_urlsafe(48),
        'GEMINI_API_KEY': values['GEMINI_API_KEY'], 'GEMINI_MODEL': values['GEMINI_MODEL'],
        'EDGE_API_KEY': edge, 'IOT_APPROVED_NODES': 'AI-SMOKE',
        'IOT_NODE_KEY_HASHES': json.dumps({'AI-SMOKE': hashlib.sha256(node_secret.encode()).hexdigest()}),
        'IOT_ENVIRONMENT': 'SIMULATED', 'REWARDS_ENABLED': 'false',
    }
    report = {'environment': 'disposable-docker', 'checks': {}}
    base = None
    headers = {}

    def call(path, body=None, method=None, extra=None, raw=None):
        data = raw if raw is not None else None if body is None else json.dumps(body).encode()
        request_headers = {'Content-Type': 'application/json', **headers, **(extra or {})}
        if 'X-Edge-Api-Key' in request_headers:
            request_headers.pop('Authorization', None)
        request = urllib.request.Request(base + path, data=data, headers=request_headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=65) as response:
                content = response.read()
                return json.loads(content) if content else None
        except urllib.error.HTTPError as error:
            # Only print the stable backend error code, never upstream text/URLs.
            code = ''
            try:
                candidate = json.load(error).get('code', '')
                if isinstance(candidate, str) and candidate.replace('_', '').isalnum() and len(candidate) < 80:
                    code = ' / ' + candidate
            except Exception:
                pass
            raise RuntimeError('Backend HTTP ' + str(error.code) + code) from None

    def passed(check):
        report['checks'][check] = 'PASS'
        print('PASS: ' + check, flush=True)

    try:
        with tempfile.TemporaryDirectory(prefix='plantsync-ai-') as temporary:
            env_file = Path(temporary) / 'api.env'
            env_file.write_text(''.join(k + '=' + v + '\n' for k, v in config.items()), encoding='utf-8')
            docker('run', '--detach', '--name', name, '--env-file', str(env_file),
                   '--publish', '127.0.0.1::8080', 'plantsync-api:local',
                   '--server.port=8080', '--server.address=0.0.0.0',
                   '--spring.datasource.url=jdbc:h2:mem:ai-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1')
            base = 'http://' + docker('port', name, '8080/tcp')
            for attempt in range(60):
                try:
                    if call('/actuator/health')['status'] == 'UP':
                        break
                except Exception:
                    time.sleep(1)
            else:
                raise RuntimeError('Disposable API startup timed out')
            email, password = uuid.uuid4().hex + '@example.test', secrets.token_urlsafe(32)
            call('/api/v1/authentication/sign-up', {'name': 'AI smoke', 'email': email,
                 'password': password, 'subscriptionPlan': 'BASIC'})
            login = call('/api/v1/authentication/sign-in', {'email': email, 'password': password})
            headers['Authorization'] = 'Bearer ' + login['token']
            me = call('/api/v1/me')
            plant = call('/api/v1/plants', {'name': 'AI test plant', 'species': 'Monstera',
                'acquisitionDate': '2026-01-01', 'humidity': 'MEDIA', 'profileId': me['profileId'],
                'humidityThresholdMin': 40, 'humidityThresholdMax': 70, 'lightThresholdMin': 50})
            node = call('/api/v1/iot/nodes', {'nodeCode': 'AI-SMOKE', 'plantId': plant['id'],
                                             'profileId': me['profileId']})
            call('/api/v1/iot/edge/sensor-readings/batch', [{'nodeId': node['id'],
                'humidityPercent': 30, 'lightPercent': 20, 'gasPercent': 10, 'airTemperature': 23}],
                extra={'X-Edge-Api-Key': edge, 'X-Node-Key': node_secret})
            def check_agent():
                conversation = call('/api/v1/agent/conversations', {})
                answer = call('/api/v1/agent/conversations/' + conversation['id'] + '/messages',
                    {'plantId': plant['id'], 'content': 'Enciende la luz de cultivo por exactamente 1 segundo.'})
                assert answer.get('reply'), 'Empty provider reply'
                passed('live_agent_reply')
                proposal = answer.get('proposal')
                assert proposal and proposal['actuator'] == 'GROW_LIGHT', 'Provider did not propose requested light action'
                assert proposal['durationSeconds'] == 1, 'Provider did not respect requested duration'
                identifier = proposal['id']
                command = call('/api/v1/actuator-command-proposals/' + identifier + '/confirm', {},
                               extra={'Idempotency-Key': identifier})
                assert command['status'] == 'QUEUED'
                passed('action_confirmation')
                # Use the actual simulator, including persistent deduplication and local cutoff.
                import sys
                sys.path.insert(0, str(ROOT / 'simulator'))
                from iot_simulator import Api, Simulator
                os.environ['EDGE_API_KEY'], os.environ['IOT_NODE_KEY'] = edge, node_secret
                api = Api(base + '/api/v1', 'AI-SMOKE')
                simulator = Simulator(api, str(Path(temporary) / 'journal.sqlite'))
                try:
                    delivered = api.commands()
                    matching = next(c for c in delivered if c['commandId'] == identifier)
                    assert simulator.accept(matching)
                    assert not simulator.accept(matching), 'Duplicate command accepted'
                    simulator.tick()
                    time.sleep(1.1)
                    simulator.tick()
                    assert call('/api/v1/actuator-commands/' + identifier)['status'] == 'EXECUTED'
                finally:
                    simulator.db.close()
                passed('simulated_delivery_deduplication_cutoff_receipt')
            def check_image():
                # Minimal valid PNG, intentionally containing no plant.
                def chunk(kind, content):
                    return struct.pack('!I', len(content)) + kind + content + struct.pack('!I', zlib.crc32(kind + content))
                image = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!IIBBBBB', 32, 32, 8, 2, 0, 0, 0))
                image += chunk(b'IDAT', zlib.compress((b'\x00' + b'\xff' * 96) * 32)) + chunk(b'IEND', b'')
                boundary = 'plantsync' + uuid.uuid4().hex
                payload = ('--' + boundary + '\r\nContent-Disposition: form-data; name="image"; filename="blank.png"\r\nContent-Type: image/png\r\n\r\n').encode() + image + ('\r\n--' + boundary + '--\r\n').encode()
                job = call('/api/v1/plant-analyses', raw=payload, extra={
                    'Content-Type': 'multipart/form-data; boundary=' + boundary, 'Idempotency-Key': uuid.uuid4().hex})
                for attempt in range(60):
                    analysis = call('/api/v1/plant-analyses/' + job['id'])
                    if analysis['status'] not in ('QUEUED', 'PROCESSING'):
                        break
                    time.sleep(2)
                assert analysis['status'] == 'INCONCLUSIVE', 'Blank image should produce INCONCLUSIVE; got ' + analysis['status'] + ' / ' + str(analysis.get('errorCode'))
                passed('live_image_upload_worker_inconclusive')
            for check, operation in [('agent_flow', check_agent), ('image_flow', check_image)]:
                try:
                    operation()
                except Exception as error:
                    reason = str(error) if isinstance(error, (AssertionError, RuntimeError)) else type(error).__name__
                    report['checks'][check] = 'FAIL: ' + reason
                    report['failure'] = 'One or more live integration checks failed'
                    print('FAIL: ' + check + ' / ' + reason, flush=True)
    except Exception as error:
        report['failure'] = str(error) if isinstance(error, (AssertionError, RuntimeError)) else type(error).__name__
        print('FAIL: ' + report['failure'], flush=True)
        return 1
    finally:
        logs = subprocess.run(['docker', 'logs', name], capture_output=True, text=True)
        log_path = ROOT / '.data' / 'live-ai-backend.log'
        log_path.parent.mkdir(exist_ok=True)
        log_path.write_text(logs.stdout + logs.stderr, encoding='utf-8')
        subprocess.run(['docker', 'rm', '--force', '--volumes', name], capture_output=True)
        destination = ROOT / '.data' / 'live-ai-verification.json'
        destination.parent.mkdir(exist_ok=True)
        destination.write_text(json.dumps(report, indent=2), encoding='utf-8')
    return 1 if report.get('failure') else 0


if __name__ == '__main__':
    raise SystemExit(main())
