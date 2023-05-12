FROM --platform=linux/amd64 ubuntu:23.04

RUN apt-get update
RUN apt-get install -y python3-pip

WORKDIR /app

COPY . /app

RUN pip3 install --no-cache-dir --break-system-packages -r requirements.txt

RUN python3 ml.py

CMD ["python3", "api.py"]
