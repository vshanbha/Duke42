import unittest
import pytest

from anomaly_detection import init_model, score

@pytest.mark.run(order=2)
class TestAnomalyDetection(unittest.TestCase):


    def setUp(self):
        # Initialize the model before running tests
        init_model()


    def test_score_valid_record(self):

        # Define a sample record (must match the structure of your training data)
        # Using numeric columns from transactions_clean.csv
        sample_record = {

            "TransactionAmount": 50.0,
            "CustomerAge": 30,
            "TransactionDuration": 120,
            "LoginAttempts": 1,
            "AccountBalance": 1000.0
        }

        # Get the anomaly score
        result = score(sample_record)


        # Assert that the result is a dictionary with the expected keys
        self.assertIsInstance(result, dict)
        self.assertIn("anomaly_score", result)
        self.assertIn("root_cause", result)

        # Assert that the anomaly score is a float
        self.assertIsInstance(result["anomaly_score"], float)


        # You might want to add more specific assertions based on your expected behavior
        # For example, check if the anomaly score is within a certain range


if __name__ == '__main__':
    unittest.main()
