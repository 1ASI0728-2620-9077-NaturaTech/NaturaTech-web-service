"""Read published plant IDs and propose REWARD_RULES without printing credentials.

Run locally: python scripts/reward-rules.py --email YOUR_LOGIN_EMAIL
Password is requested securely. No account or server data is modified.
"""
import argparse
import getpass
import json
import math
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--email', required=True)
    args = parser.parse_args()
    base = 'https://plantsync-backend.onrender.com/api/v1'
    token = None

    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None

    opener = urllib.request.build_opener(NoRedirect())

    def call(path, body=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(base + path,
            data=None if body is None else json.dumps(body).encode(), headers=headers)
        with opener.open(request, timeout=60) as response:
            return json.load(response)

    try:
        login = call('/authentication/sign-in', {'email': args.email, 'password': getpass.getpass('Password (not displayed): ')})
        token = login['token']
        me = call('/me')
        try:
            plants = call('/plants/by-profile/' + str(me['profileId']))
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
            plants = []
        try:
            nodes = call('/iot/nodes/by-profile/' + str(me['profileId']))
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
            nodes = []
        linked = {node.get('plantId') for node in nodes}
        rules = {}
        for plant in plants:
            plant_id = plant['id']
            print('Published plant:', plant_id, 'linkedNode:', plant_id in linked)
            values = [plant.get(key) for key in ('humidityThresholdMin', 'humidityThresholdMax', 'lightThresholdMin')]
            if plant_id not in linked:
                print('  Skipped: no linked node')
                continue
            if not all(type(v) in (int, float) and math.isfinite(v) and 0 <= v <= 100 for v in values) or values[0] > values[1]:
                print('  Skipped: configure valid care thresholds first')
                continue
            rules[str(plant_id)] = dict(zip(('humidityMin', 'humidityMax', 'lightMin'), values))
        print('Candidate REWARD_RULES (review before enabling):')
        print(json.dumps(rules, separators=(',', ':')))
        print('Rules use published plant IDs, not node, profile or user IDs.')
        print('Existing challenges keep their enrolled thresholds; environment changes affect new enrollments.')
    except urllib.error.HTTPError as error:
        print('Published API returned HTTP', error.code, '(response body omitted)')
        return 1
    except Exception as error:
        print('Read-only check failed:', type(error).__name__, '(details omitted)')
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
