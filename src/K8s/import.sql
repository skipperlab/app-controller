insert into palette (id, name, context, config_template)
values (10001
, 'Connect'
, 'apiVersion: v1
kind: ConfigMap
metadata:
  name: connect-app-1
data:
  key-name: "value"

  connect-distributed.properties: |
    bootstrap.servers=141.164.42.200:9092
    plugin.path=/opt/connectors
    group.id=connect-app-1

    key.converter=org.apache.kafka.connect.json.JsonConverter
    key.converter.schemas.enable=true
    value.converter=org.apache.kafka.connect.json.JsonConverter
    value.converter.schemas.enable=true

    offset.storage.topic=connect-offsets
    offset.storage.replication.factor=1
    #offset.storage.partitions=25
    offset.flush.interval.ms=10000

    config.storage.topic=connect-configs
    config.storage.replication.factor=1
    status.storage.topic=connect-status
    status.storage.replication.factor=1
    #status.storage.partitions=5

    #listeners=HTTP://:8083
    #rest.advertised.host.name=
    #rest.advertised.port=
    #rest.advertised.listener=

  connect-log4j.properties: |
    log4j.rootLogger=INFO, stdout, connectAppender

    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout

    log4j.appender.connectAppender=org.apache.log4j.DailyRollingFileAppender
    log4j.appender.connectAppender.DatePattern=\'.\'yyyy-MM-dd-HH
    log4j.appender.connectAppender.File=${kafka.logs.dir}/connect.log
    log4j.appender.connectAppender.layout=org.apache.log4j.PatternLayout

    connect.log.pattern=[%d] %p %X{connector.context}%m (%c:%L)%n

    log4j.appender.stdout.layout.ConversionPattern=${connect.log.pattern}
    log4j.appender.connectAppender.layout.ConversionPattern=${connect.log.pattern}

    log4j.logger.org.apache.zookeeper=ERROR
    log4j.logger.org.reflections=ERROR
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: connect-app-1
  labels:
    app: connect-app-1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: connect-app-1
  template:
    metadata:
      labels:
        app: connect-app-1
    spec:
      containers:
        - name: connect-app-1
          image: skipperlab/connect:v3.3.2
          command: ["connect-distributed.sh", "config/connect-distributed.properties"]
          ports:
            - containerPort: 8083
          volumeMounts:
            - name: config-app
              mountPath: "/opt/kafka/config"
              readOnly: true
            - name: connectors-app
              mountPath: "/opt/connectors"
              readOnly: true
      volumes:
        - name: config-app
          configMap:
            name: connect-app-1
            items:
              - key: "connect-log4j.properties"
                path: "connect-log4j.properties"
              - key: "connect-distributed.properties"
                path: "connect-distributed.properties"
        - name: connectors-app
          persistentVolumeClaim:
            claimName: pvc-connectors'
, '["bootstrap.servers", "app.name", "key.converter", "value.converter"]');