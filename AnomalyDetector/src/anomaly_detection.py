import pandas as pd
from sklearn.ensemble import IsolationForest

def train_isolation_forest(df, contamination='auto'):
    """
    Trains an Isolation Forest model on the given data.
    """
    model = IsolationForest(contamination=contamination)
    model.fit(df)
    return model

def predict_anomalies(model, df):
    """
    Predicts anomalies using the trained Isolation Forest model.
    """
    anomalies = model.predict(df)
    return anomalies