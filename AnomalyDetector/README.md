# Anomaly Detection Project

## Overview

This project implements anomaly detection techniques, primarily using the Isolation Forest algorithm, to identify unusual transactions in a dataset.

## Project Structure
```
AnomalyDetectionProject/
├── data/                      # Raw data storage
├── src/                       # Source code
├── models/                    # Saved models
├── tests/                     # Unit and integration tests
├── requirements.txt           # Project dependencies
└── README.md                  # Project documentation
```
This project implements anomaly detection techniques, primarily using the Isolation Forest algorithm, to identify unusual transactions in a dataset.

This project implements anomaly detection techniques, primarily using the Isolation Forest algorithm, to identify unusual transactions in a dataset.

## Virtual Environment
To set up and manage a virtual environment for this project, follow these steps:

1.  **Create a virtual environment:**

    Navigate to the project directory in your terminal and run:

    ```bash
    python -m venv venv
    ```

    This command creates a new virtual environment named `venv` in your project directory.

2.  **Activate the virtual environment:**

    *   On Windows, run:

        ```bash
        venv\Scripts\activate
        ```

    *   On macOS and Linux, run:

        ```bash
        source venv/bin/activate
        ```

        source venv/bin/activate
        ```

    With the virtual environment activated, install the project dependencies using pip:

    ```bash
    pip install -r requirements.txt
    ```

    This command installs all the packages listed in the `requirements.txt` file.

## Testing

The `tests/` directory contains unit and integration tests to ensure the reliability and correctness of the code.

### Test Files

*   `test_data_preprocessing.py`: Contains tests for the data loading, cleaning, and preprocessing functions in `src/data_preprocessing.py`.
*   `test_anomaly_detection.py`: Contains tests for the anomaly detection model training and prediction functions in `src/anomaly_detection.py`.

### Running Tests

To run the tests, use the following command:
```bash
pytest
```
