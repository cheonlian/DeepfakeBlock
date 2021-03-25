FROM python:3.6

ENV PYTHONUNBUFFERED=1

WORKDIR /app

ADD . /app

COPY api/requirements.txt /app/requirements.txt
COPY api/requirements2.txt /app/requirements2.txt

RUN pip install -r requirements.txt
RUN pip install -r requirements2.txt

CMD ["jupyter-notebook", "--allow-root" ,"--port=8888" ,"--no-browser" ,"--ip=0.0.0.0"]

COPY . /app