import unittest

import pytest

from anomaly_detection import init_model, score, MODEL_FILE, load_model
import os

ANOMALY_THRESHOLD = -0.1  # Adjust this value based on your model's score distribution

@pytest.mark.run(order=2)
class TestAnomalyDetection(unittest.TestCase):


    def setUp(self):
        # Initialize the model before running each test
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
    def test_score_known_anomaly(self):
        """
        Test that a known anomalous record is detected as an anomaly.
        """
        # Define an anomalous record
        anomalous_record = {
            "TransactionAmount": 10000.0,
            "CustomerAge": 18,
            "TransactionDuration": 10,
            "LoginAttempts": 5,
            "AccountBalance": 50.0
        }

        result = score(anomalous_record)

        self.assertIsInstance(result, dict)
        self.assertIn("anomaly_score", result)
        self.assertIn("root_cause", result)

        self.assertIsInstance(result["anomaly_score"], float)
        self.assertLess(result["anomaly_score"], ANOMALY_THRESHOLD, "Anomaly score should be below the threshold.")

    def test_score_normal_record(self):
        """
        Test that a normal record is not detected as an anomaly.
        """
        # Define a normal record
        normal_record = {
            "TransactionAmount": 20.0,
            "CustomerAge": 40,
            "TransactionDuration": 60,
            "LoginAttempts": 1,
            "AccountBalance": 5000.0
        }

        result = score(normal_record)

        self.assertIsInstance(result["anomaly_score"], float)
        self.assertGreaterEqual(result["anomaly_score"], ANOMALY_THRESHOLD, "Anomaly score should be above the threshold.")

    def test_load_model(self):
        """
        Test that the model can be loaded from file.
        """
        # Ensure the model file exists
        self.assertTrue(os.path.exists(MODEL_FILE), f"Model file {MODEL_FILE} does not exist.")

        # Load the model
        load_model(MODEL_FILE)

        # Check that the model is loaded
        from anomaly_detection import MODEL
        self.assertIsNotNone(MODEL, "Model should be loaded.")

        # You could also add a check to ensure the loaded model
        # produces the same scores as the original model, if needed.
        # For example, score a sample record and compare the results.





if __name__ == '__main__':
    unittest.main()
