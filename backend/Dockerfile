FROM python:3.12-slim

WORKDIR /app

# Copy requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the source code
COPY app .

# run the server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]