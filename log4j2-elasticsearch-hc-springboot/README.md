# log4j2-elasticsearch HC example

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

Example Spring Boot application with log delivery to Elasticsearch using [log4j2-elasticsearch](https://github.com/rfoltyns/log4j2-elasticsearch).

Some advanced (optional) features were configured to demonstrate capabilities.

## Usage

### Build
```shell
mvn clean install
```

### Run

Ensure that ES is running at `localhost:9200` or change the [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch-examples/blob/master/log4j2-elasticsearch-hc-springboot/src/main/resources/log4j2.xml) file.

```shell
java -jar -Dhostname=localhost target/log4j2-elasticsearch-hc-springboot-0.0.1-SNAPSHOT.jar
```

### Verify

```shell
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_template/log4j2-elasticsearch-hc-example-template?pretty
curl http://localhost:9200/log4j2-elasticsearch-hc-test*/_search?pretty
```

