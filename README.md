## Агрегация - гистограмма по времени для логов ##

```dtd
GET /logstash*/_search
{
  "size": 0,
  "aggs": {
    "logs_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "fixed_interval": "1h"  
      }
    }
  }
}
```

## Агрегация - гистограмма по авторам ##
```dtd
GET /news/_search
{
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "author"
      }
    }
  }
}
```
## Запрос с использованием скрипта ##
```dtd
GET /news/_search
{
  "query": {
    "script_score": {
      "query": {"match_all": {}},
      "script": {
        "source": "doc['title'].value.length()"
      }
    }
  }
}
```
## Запрос на обновление документа ## 
```dtd
POST /news/_update/9c5247c775e4d62ecbd638d5f654e05d
{
  "doc": {
    "author": "Kristina Degteryova"
  }
}
```
## Сложный поисковый запрос (boolQuery)
```dtd
GET /news/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "назвала" }},
        { "match": { "text": "игра" }}
      ],
      "filter": [
        { "range": { "date": { "gte": "2024-01-01" }}}
      ]
    }
  }
}
```
## Запрос MultiGet ##
```dtd
GET /news/_mget
{
  "docs": [
    { "_index": "news", "_id": "ab72e4a20427c0b020878b3f44d3f16e" },
    { "_index": "news", "_id": "857432056faf14de6bae52060ece6c03" }
  ]
}
```
## Понотекстовые запросы ##
```dtd
GET /news/_search
{
  "query": {
    "match": {
      "text": {
        "query": "стиль актуальность",
        "operator": "and"
      }
    }
  }
}
```
```dtd
GET /news/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "text": "мода" } }
      ],
      "filter": {
        "range": {
          "date": {
            "gte": "2024-05-10"
          }
        }
      }
    }
  }
}
```

## Поиск по нескольким полям ##
```dtd
GET /news/_search
{
  "query": {
    "multi_match": {
      "query": "Москва",
      "fields": [ "title", "text", "author" ]
    }
  }
}
```