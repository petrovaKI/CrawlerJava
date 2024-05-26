package org.parser;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final String BASE_URL = "https://vm.ru";
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {

        final int NUM_THREADS = 5;
        ThreadRunner threadRunner = new ThreadRunner(NUM_THREADS);

        RabbitProducer rabbitProducer = new RabbitProducer();
        try {
            rabbitProducer.sendLinksToRabbitMQ(BASE_URL);
        } catch (IOException | InterruptedException | TimeoutException e) {
            logger.error(e.getMessage());
        }

        AtomicInteger totalLinksCount = new AtomicInteger();
        threadRunner.runTasks(() -> {
            try {
                RabbitConsumer rabbitConsumer = new RabbitConsumer();
                totalLinksCount.addAndGet(rabbitConsumer.getLinksFromRabbitMQ());
            } catch (IOException | TimeoutException e) {
                logger.error(e.getMessage());
            }
            return null;
        });
        ElasticConsumer elasticConsumer = new ElasticConsumer();
        ThreadRunner elasticThreadRunner = new ThreadRunner(1);
        elasticThreadRunner.runTasks(() -> {
            try {
                elasticConsumer.startConsumingAndIndexing();
            } catch (IOException | TimeoutException e) {
                logger.error(e.getMessage());
            }
            return null;
        });
        threadRunner.awaitTermination();
        elasticThreadRunner.awaitTermination();

        logger.debug("Received " + totalLinksCount.get() + " links.");
        elasticConsumer.closeConnection();
    }
}