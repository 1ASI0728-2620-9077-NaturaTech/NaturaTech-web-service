import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock
from iot_simulator import Simulator
from prepare import prepare


class SimulatorTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.path = str(Path(self.directory.name) / 'journal.sqlite')
        self.clock = Mock(return_value=1000)
        self.api = Mock()
        self.sim = Simulator(self.api, self.path, self.clock)
        self.command = {'commandId': 'command-123', 'actuator': 'WATER_PUMP', 'durationSeconds': 5, 'expiresAt': '1970-01-01T00:18:20Z'}

    def tearDown(self):
        self.sim.db.close()
        self.directory.cleanup()

    def test_duplicate_cannot_extend_deadline_or_execute_twice(self):
        self.assertTrue(self.sim.accept(self.command))
        self.clock.return_value = 1002
        self.assertFalse(self.sim.accept(self.command))
        self.sim.tick()
        self.api.receipt.assert_called_with('command-123', 'ACKNOWLEDGED')
        self.clock.return_value = 1005
        self.sim.tick()
        self.api.receipt.assert_called_with('command-123', 'EXECUTED')
        self.assertEqual(45, self.sim.reading(1)['humidityPercent'])

    def test_rejects_expired_and_unbounded_commands(self):
        self.clock.return_value = 1100
        self.assertFalse(self.sim.accept(self.command))
        self.clock.return_value = 1000
        self.command['durationSeconds'] = 11
        self.assertFalse(self.sim.accept(self.command))

    def test_cutoff_survives_receipt_network_failure(self):
        self.sim.accept(self.command)
        self.api.receipt.side_effect = ConnectionError()
        self.clock.return_value = 1006
        self.sim.tick()
        self.assertEqual(45, self.sim.reading(1)['humidityPercent'])
        self.api.receipt.side_effect = None
        self.sim.tick()
        self.api.receipt.assert_called_with('command-123', 'EXECUTED')

    def test_restart_reports_failure_without_replaying(self):
        self.sim.accept(self.command)
        self.sim.db.close()
        self.sim = Simulator(self.api, self.path, self.clock)
        self.assertFalse(self.sim.accept(self.command))
        self.sim.tick()
        self.api.receipt.assert_called_with('command-123', 'FAILED')

    def test_preparation_preserves_gemini_and_existing_device_secret(self):
        path = Path(self.directory.name) / '.env.local'
        path.write_text('GEMINI_API_KEY=test-placeholder\nGEMINI_MODEL=gemini-flash-latest\n')
        prepare(path, 'SIM-001')
        first = path.read_text()
        prepare(path, 'SIM-001')
        self.assertEqual(first, path.read_text())
        self.assertIn('GEMINI_API_KEY=test-placeholder', first)
        self.assertIn('IOT_ENVIRONMENT=SIMULATED', first)


if __name__ == '__main__':
    unittest.main()
