# log4j2-elasticsearch AHC standalone, no-Log4j2 example

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

Example standalone application with log delivery to Elasticsearch using [log4j2-elasticsearch-ahc](https://github.com/rfoltyns/log4j2-elasticsearch-ahc).

Some advanced (optional) features were configured to demonstrate capabilities.

## Usage

### Build
```shell
mvn clean install
```

### Run

Ensure that ES is running at `localhost:9200` or change the [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch-examples/blob/master/log4j2-elasticsearch-hc-springboot/src/main/resources/log4j2.xml) file.

```shell
java -jar target/log4j2-elasticsearch-ahc-one-jar-0.0.1-SNAPSHOT.one-jar.jar
```

### Verify

```shell
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_template/log4j2-elasticsearch-ahc-one-jar?pretty
curl http://localhost:9200/log4j2-elasticsearch-ahc-one-jar*/_search?pretty
```

