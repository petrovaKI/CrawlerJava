package org.parser;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import entities.Message;
import entities.News;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.parser.Parser.startParsing;

public class RabbitConsumer {
    private final Connection connection;
    private final Channel channel;
    private AtomicInteger totalLinks = new AtomicInteger(0);
    public static String CONSUMER_QUEUE_NAME = "consumer_queue";
    private final Object lock = new Object();
    private static final Logger logger = LogManager.getLogger(RabbitConsumer.class);


    public RabbitConsumer() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(RabbitProducer.PRODUCER_QUEUE_NAME, false, false, false, null);
        channel.queueDeclare(CONSUMER_QUEUE_NAME, false, false, false, null);
    }

    public void getLinksFromRabbitMQ() {
        try {
            Gson gson = new Gson();
            long timeoutMillis = 60000;
            long lastMessageTime = System.currentTimeMillis();

            while (true) {
                //использован basicGet, так как нет необходимости постоянно слушать очередь producer-a
                //на момент выполнения этого метода, она полностью готова к использованию
                GetResponse response = channel.basicGet(RabbitProducer.PRODUCER_QUEUE_NAME, false);
                if (response == null) {
                    if (System.currentTimeMillis() - lastMessageTime > timeoutMillis) {
                        break;
                    }
                    Thread.sleep(1000);
                    continue;
                }

                lastMessageTime = System.currentTimeMillis();

                byte[] body = response.getBody();
                long tag = response.getEnvelope().getDeliveryTag();
                String message = new String(body, StandardCharsets.UTF_8);

                logger.debug("Message received " + message);

                Message msg = gson.fromJson(message, Message.class);
                if (msg == null || msg.getLink() == null) {
                    logger.error("Invalid message format or missing link");
                    channel.basicNack(tag, false, false);
                    continue;
                }

                try {
                    String link = msg.getLink();
                    News news = startParsing(link);
                    if (news != null) {
                            synchronized (lock){
                            String newsJson = gson.toJson(news);
                            channel.basicPublish("", CONSUMER_QUEUE_NAME, null, newsJson.getBytes());
                            logger.debug("News sent to queue: " + news.getLink());
                        }
                    }
                    channel.basicAck(tag, false);
                    totalLinks.incrementAndGet();
                    logger.debug("Message deleted " + message);
                } catch (Exception ex) {
                    logger.error(System.err);
                    try {
                        channel.basicNack(tag, false, true);
                    } catch (IOException ioException) {
                        logger.error(System.err);
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
    public int getTotalLinks() {
        return totalLinks.get();
    }
}

