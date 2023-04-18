FROM python:3.11.3-slim-buster

WORKDIR /app

COPY . /app

RUN pip3 install --no-cache-dir -r requirements.txt

RUN python3 ml.py

CMD ["python3", "api.py"]
