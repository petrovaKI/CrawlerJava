package org.parser;

import models.News;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class ElasticConsumer {
    private final Gson gson;
    private final Connection connection;
    private final Channel channel;
    private final RestHighLevelClient client;
    private final String indexName;
    private static final Logger logger = LogManager.getLogger(ElasticConsumer.class);

    public ElasticConsumer() throws IOException, TimeoutException {
        gson = new Gson();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.basicQos(1);

        String elasticsearchHost = "127.0.0.1";
        int elasticsearchPort = 9200;
        indexName = "news";
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, "http")));

        createIndex();
    }
    public void startConsumingAndIndexing() throws IOException, TimeoutException {
        channel.basicConsume(RabbitConsumer.CONSUMER_QUEUE_NAME, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                News news = gson.fromJson(message, News.class);

                try {
                    if (!documentExists(news.getId())) {
                        insertDocument(news);
                    } else {
                        logger.warn("Document exists: " + news.getId());
                    }
                    channel.basicAck(envelope.getDeliveryTag(), false);
                } catch (IOException e) {
                    logger.error("Failed to process message: " + e.getMessage());
                    channel.basicNack(envelope.getDeliveryTag(), false, true);
                }
            }
        });
    }

    public void createIndex() throws IOException {
        if (!indexExists()) {
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            request.mapping(createIndexMapping(), XContentType.JSON);
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            if (createIndexResponse.isAcknowledged()) {
                logger.debug("Index created successfully: " + indexName);
            } else {
                logger.error("Failed to create index: " + indexName);
            }
        } else {
            logger.warn("Index already exists: " + indexName);
        }
    }

    public void insertDocument(News news) {
        try {
            IndexRequest request = new IndexRequest(indexName);
            request.id(news.getId());
            request.source(gson.toJson(news), XContentType.JSON);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            if (response.getResult() == DocWriteResponse.Result.CREATED || response.getResult() == DocWriteResponse.Result.UPDATED) {
                logger.debug("Document inserted successfully: " + news.getId());
            } else {
                logger.error("Failed to insert document: " + news.getId());
            }
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
    }

    public boolean documentExists(String id) throws IOException {
        return client.exists(new GetRequest(indexName, id), RequestOptions.DEFAULT);
    }

    public String createIndexMapping() {
        return """
                {
                  "properties": {
                    "title": {
                      "type": "text",
                      "fielddata": true
                    },
                    "date": {
                      "type": "text",
                      "fielddata": true
                    },
                    "text": {
                      "type": "text",
                      "fielddata": true
                    },
                    "link": {
                      "type": "text"
                    },
                    "author": {
                      "type": "text",
                      "fielddata": true
                    },
                    "id": {
                      "type": "keyword"
                    }
                  }
                }""";
    }

    public boolean indexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }
    public void closeConnection() throws IOException, TimeoutException {
        channel.close();
        connection.close();
        client.close();

    }
}