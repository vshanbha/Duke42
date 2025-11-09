import pandas as pd
import os
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

DATA_DIR = 'data'
RAW_DATA_DIR = os.path.join(DATA_DIR, 'raw')
PROCESSED_DATA_DIR = os.path.join(DATA_DIR, 'processed')

TRANSACTIONS_FILE = os.path.join(RAW_DATA_DIR, 'transactions.csv')
CLEAN_TRANSACTIONS_FILE = os.path.join(PROCESSED_DATA_DIR, 'transactions_clean.csv')

def prepare_transactions_data():
    """
    Loads, cleans, and validates the transactions data, saving the cleaned
    version to a new file.
    """
    logging.info(f"Loading transactions data from {TRANSACTIONS_FILE}")
    try:
        df = pd.read_csv(TRANSACTIONS_FILE)
    except FileNotFoundError:
        logging.error(f"File not found: {TRANSACTIONS_FILE}")
        return None

    logging.info("Sample columns from the dataset:")
    logging.info(df.head())

    # Identify numeric columns that are not of type float64 or int64
    numeric_cols = df.select_dtypes(include=['number']).columns
    for c in df.columns:
        if c in numeric_cols:
            if df[c].dtype not in ['float64', 'int64']:
                logging.info(f"Casting column {c} to numeric")
                df[c] = pd.to_numeric(df[c], errors='coerce')

    # Save the cleaned data
    if not os.path.exists(PROCESSED_DATA_DIR):
        os.makedirs(PROCESSED_DATA_DIR)

    logging.info(f"Saving cleaned transactions data to {CLEAN_TRANSACTIONS_FILE}")
    df.to_csv(CLEAN_TRANSACTIONS_FILE, index=False)

    logging.info("Data preparation complete.")
    return df

if __name__ == "__main__":
    # Ensure the 'processed' directory exists
    if not os.path.exists(PROCESSED_DATA_DIR):
        os.makedirs(PROCESSED_DATA_DIR)

    # Run the data preparation
    prepared_df = prepare_transactions_data()