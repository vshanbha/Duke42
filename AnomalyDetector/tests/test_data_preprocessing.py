import unittest
import pytest
import pandas as pd
import os
from data_preprocessing import prepare_transactions_data, CLEAN_TRANSACTIONS_FILE

@pytest.mark.run(order=1)
class TestDataPreprocessing(unittest.TestCase):

    def setUp(self):
        """
        Set up test environment by preparing the transactions data.
        """
        self.cleaned_df = prepare_transactions_data()
        self.assertTrue(os.path.exists(CLEAN_TRANSACTIONS_FILE), "Cleaned transactions file does not exist.")
        self.assertIsInstance(self.cleaned_df, pd.DataFrame, "The result should be a Pandas DataFrame.")

    def test_numeric_columns_are_numeric(self):
        """
        Test that all numeric columns are of type float64 or int64.
        """
        numeric_cols = self.cleaned_df.select_dtypes(include=['number']).columns
        for col in numeric_cols:
            self.assertIn(self.cleaned_df[col].dtype.name, ['float64', 'int64'],
                            f"Column {col} is not of type float64 or int64.")

    def test_no_missing_values_in_numeric_columns(self):
        """
        Test that there are no missing values in numeric columns after cleaning.
        """
        numeric_cols = self.cleaned_df.select_dtypes(include=['number']).columns
        for col in numeric_cols:
            self.assertFalse(self.cleaned_df[col].isnull().any(),
                             f"Column {col} contains missing values.")

if __name__ == '__main__':
    unittest.main()
