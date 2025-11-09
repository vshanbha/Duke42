import pandas as pd
from sklearn.ensemble import IsolationForest
import os
import json

MODEL_FILE = os.path.join('models', 'anomaly_model.joblib')
MODEL = None
CLEAN_TRANSACTIONS_FILE = os.path.join('data', 'processed', 'transactions_clean.csv')

def init_model():
    """
    Initializes the Isolation Forest model.
    """
    global MODEL

    # Load the cleaned transactions data
    try:
        df = pd.read_csv(CLEAN_TRANSACTIONS_FILE)
    except FileNotFoundError:
        print(f"Error: {CLEAN_TRANSACTIONS_FILE} not found. Make sure to run data_preprocessing.py first.")
        return

    # Select numeric columns
    numeric_cols = df.select_dtypes(include=['number']).columns
    df_numeric = df[numeric_cols]

    # Train a small Isolation Forest model
    MODEL = IsolationForest(n_estimators=100, contamination='auto', random_state=42) # Adjust parameters as needed
    MODEL.fit(df_numeric)
    
    # Save the model
    save_model(MODEL, MODEL_FILE)

def score(record: dict) -> dict:
    """
    Scores a record using the trained Isolation Forest model.
    """
    global MODEL
    if MODEL is None:
        return {"anomaly_score": 0.0, "root_cause": "Model not initialized"}

    record_df = pd.DataFrame([record])
    anomaly_score = MODEL.decision_function(record_df) # Lower score indicates more anomalous

    return {"anomaly_score": float(anomaly_score[0]), "root_cause": "Isolation Forest"}

def save_model(model, filename: str):
    """
    Saves the trained Isolation Forest model to a file.
    """
    import joblib
    joblib.dump(model, filename)
    print(f"Model saved to {filename}")

def load_model(filename: str):
    """
    Loads the trained Isolation Forest model from a file.
    """
    global MODEL
    import joblib
    MODEL = joblib.load(filename)
    print(f"Model loaded from {filename}")