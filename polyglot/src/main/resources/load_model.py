import json
import logging
import time
from textblob import TextBlob

logger = logging.getLogger(__name__)

# Global cache for loaded models
_anomaly_model = None

# Define the feature order expected by the model.
# This MUST match the order of columns used during model training.
ANOMALY_FEATURE_ORDER = [
    "TransactionAmount", "CustomerAge", "TransactionDuration", 
    "LoginAttempts", "AccountBalance"
]


def detect_anomaly(record):
    """Detect anomalies in a transaction record using Isolation Forest.
    
    Args:
        record: Dictionary of transaction features (e.g., TransactionAmount, CustomerAge, etc.)
                or JSON string representing the record.
    
    Returns:
        JSON string containing:
          - "anomaly_score": float decision function score (lower = more anomalous)
          - "classification": "anomaly" if score < -0.1, "normal" otherwise
          - "took_ms": execution time in milliseconds
        On error, returns JSON with "error" key describing the failure.
    """
    global _anomaly_model
    
    try:
        start = time.time()
        
        # Parse input if it's a JSON string
        if isinstance(record, str):
            record = json.loads(record)
        
        if not isinstance(record, dict):
            raise TypeError("record must be a dictionary or JSON string")
        
        # Lazy-load the anomaly model on first call
        if _anomaly_model is None:
            _anomaly_model = _load_anomaly_model()
        
        if _anomaly_model is None:
            return json.dumps({"error": "Anomaly model could not be loaded"})
        
        # Extract record values in the correct, consistent order
        try:
            record_values = [record[feature] for feature in ANOMALY_FEATURE_ORDER]
        except KeyError as e:
            raise ValueError(f"Missing feature in record: {e}")
        
        # Score the record using the model's decision function
        anomaly_score = _anomaly_model.decision_function([record_values])[0]
        
        # Classify as anomaly or normal
        classification = "anomaly" if anomaly_score < -0.1 else "normal"
        
        took_ms = (time.time() - start) * 1000.0
        logger.debug(f"Anomaly detection took {took_ms:.2f} ms")
        
        return json.dumps({
            "score": float(anomaly_score),
            "classification": classification,
            "took_ms": round(took_ms, 2)
        })
    except Exception as e:
        logger.exception("Anomaly detection failed")
        return json.dumps({"error": str(e)})

def _load_anomaly_model():
    """Load the Isolation Forest model from joblib file.
    
    Returns:
        The loaded sklearn IsolationForest model, or None if loading fails.
    """
    try:
        import joblib
        import os
        
        # Try multiple possible paths to find the model file
        possible_paths = [
            # Path when running from the polyglot project root
            "anomaly_model.joblib",
        ]
        
        for model_path in possible_paths:
            if os.path.exists(model_path):
                logger.info(f"Loading anomaly model from {os.path.abspath(model_path)}")
                return joblib.load(model_path)
        
        logger.error(f"Anomaly model not found in any of the expected paths: {possible_paths}")
        return None
    except Exception as e:
        logger.exception(f"Failed to load anomaly model: {e}")
        return None

def analyze_sentiment(text):
    """Analyze sentiment of the provided text.
    
    Args:
        text: Input text to analyze (will be coerced to string if needed).
    
    Returns:
        JSON string containing:
          - "score": float polarity score (-1.0 to 1.0)
          - "classification": "positive", "neutral", or "negative"
          - "took_ms": execution time in milliseconds
        On error, returns JSON with "error" key describing the failure.
    """
    try:
        start = time.time()
        
        # Normalize input
        if text is None:
            text = ""
        text = str(text).strip()
        
        # Analyze sentiment
        analysis = TextBlob(text)
        sentiment_score = analysis.sentiment.polarity
        sentiment_classification = (
            "positive" if sentiment_score > 0.1 else
            "negative" if sentiment_score < -0.1 else
            "neutral"
        )
        
        took_ms = (time.time() - start) * 1000.0
        logger.debug(f"Sentiment analysis took {took_ms:.2f} ms")
        
        return json.dumps({
            "score": sentiment_score,
            "classification": sentiment_classification,
            "took_ms": round(took_ms, 2)
        })
    except Exception as e:
        logger.exception("Sentiment analysis failed")
        return json.dumps({"error": str(e)})