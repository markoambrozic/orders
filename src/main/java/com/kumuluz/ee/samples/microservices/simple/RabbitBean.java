package com.kumuluz.ee.samples.microservices.simple;

import com.kumuluz.ee.fault.tolerance.annotations.CommandKey;
import com.kumuluz.ee.fault.tolerance.annotations.GroupKey;
import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.Logger;
import com.kumuluz.ee.logs.cdi.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.eclipse.microprofile.faulttolerance.*;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.time.temporal.ChronoUnit;

@RequestScoped
@Bulkhead
@GroupKey("rabbit")
@Log
public class RabbitBean {
    private static final Logger LOG = LogManager.getLogger(RabbitBean.class.getName());

    private final static String QUEUE_NAME = "orders";

    @CircuitBreaker
    @Fallback(fallbackMethod = "insertOrderIntoQueueFallback")
    @CommandKey("queue-order")
    @Timeout(value = 1, unit = ChronoUnit.MICROS)
    public boolean insertOrderIntoQueue(String rabbitMqUri, String orderJSON) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(
                rabbitMqUri
        );
        factory.setHandshakeTimeout(60000);

        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME, null, orderJSON.getBytes("UTF-8"));
            LOG.trace("Order sent to queue: "+orderJSON);
        }

        return true;
    }

    public boolean insertOrderIntoQueueFallback(String rabbitMqUri, String orderJSON) {
        LOG.trace("Order queueing failed");
        return true;
    }

    @GET
    @Path("/getOrdersFromQueue")
    public boolean getOrdersFromQueue(String rabbitMqUri) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqUri);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            LOG.trace("Processing order: "+message);
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

        return true;
    }
}
