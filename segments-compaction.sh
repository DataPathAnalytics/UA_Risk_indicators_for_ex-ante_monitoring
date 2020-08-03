#!/bin/bash

CURRENT_DATE='date +%F';

curl --location --request POST 'http://localhost:8090/druid/indexer/v1/task' \
--header 'Content-Type: application/json' \
--data-raw '{
    "type": "compact",
    "dataSource": "tender_indicators_v1",
    "interval": "2015-01-01/2018-01-01",
    "segmentGranularity": "day"
}';

curl --location --request POST 'http://localhost:8090/druid/indexer/v1/task' \
--header 'Content-Type: application/json' \
--data-raw '{
    "type": "compact",
    "dataSource": "tender_indicators_v1",
    "interval": "2018-01-01/2019-01-01",
    "segmentGranularity": "day"
}';

curl --location --request POST 'http://localhost:8090/druid/indexer/v1/task' \
--header 'Content-Type: application/json' \
--data-raw '{
    "type": "compact",
    "dataSource": "tender_indicators_v1",
    "interval": "2019-01-01/2020-01-01",
    "segmentGranularity": "day"
}';

curl --location --request POST 'http://localhost:8090/druid/indexer/v1/task' \
--header 'Content-Type: application/json' \
--data-raw '{
    "type": "compact",
    "dataSource": "tender_indicators_v1",
    "interval": "2020-01-01/2020-03-01",
    "segmentGranularity": "day"
}';