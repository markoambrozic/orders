package com.kumuluz.ee.samples.microservices.simple;

import com.kumuluz.ee.samples.microservices.simple.Models.Order;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.Logger;
import com.kumuluz.ee.logs.cdi.Log;

@Path("/orders")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrdersResource {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private OrdersProperties ordersProperties;

    private static final Logger LOG = LogManager.getLogger(OrdersResource.class.getName());

    private final static String QUEUE_NAME = "orders";

    @GET
    public Response getOrders() {

        TypedQuery<Order> query = em.createNamedQuery("BookOrder.findAll", Order.class);

        List<Order> orders = query.getResultList();

        return Response.ok(orders).build();
    }

    @GET
    @Path("/{id}")
    public Response getOrder(@PathParam("id") Integer id) {

        Order o = em.find(Order.class, id);

        if (o == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        return Response.ok(o).build();
    }

    @POST
    @Path("/completeOrder")
    public Response placeOrder(String orderJSON) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri("amqps://admin:admin@portal-ssl676-94.bmix-eu-gb-yp-76826436-8167-446f-8f80-b63de37b51aa.3782795196.composedb.com:16865/bmix-eu-gb-yp-76826436-8167-446f-8f80-b63de37b51aa");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        factory.setHandshakeTimeout(60000);

        try {
            try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.basicPublish("", QUEUE_NAME, null, orderJSON.getBytes("UTF-8"));
                LOG.trace("Order sent to queue: "+orderJSON);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.GATEWAY_TIMEOUT).entity(e).build();
        }

        LOG.trace("New order created.");

        return Response.status(Response.Status.CREATED).entity(orderJSON).build();
    }

    @GET
    @Path("/getOrdersFromQueue")
    public Response getOrdersFromQueue() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqps://admin:admin@portal-ssl676-94.bmix-eu-gb-yp-76826436-8167-446f-8f80-b63de37b51aa.3782795196.composedb.com:16865/bmix-eu-gb-yp-76826436-8167-446f-8f80-b63de37b51aa");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            LOG.trace("Processing order: "+message);
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

        return Response.status(Response.Status.OK).build();
    }
}
