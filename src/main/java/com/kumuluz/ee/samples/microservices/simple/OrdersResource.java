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
@Log
public class OrdersResource {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private OrdersProperties ordersProperties;

    private static final Logger LOG = LogManager.getLogger(OrdersResource.class.getName());

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
    public Response placeOrder(String orderJSON) throws Exception {
        RabbitBean rabbitMq = new RabbitBean();
        rabbitMq.insertOrderIntoQueue(ordersProperties.getRabbitMqUri(), orderJSON);

        LOG.trace("New order created.");

        return Response.status(Response.Status.CREATED).entity(orderJSON).build();
    }

    @GET
    @Path("/getOrdersFromQueue")
    public Response getOrdersFromQueue() throws Exception {
        RabbitBean rabbitMq = new RabbitBean();
        rabbitMq.getOrdersFromQueue(ordersProperties.getRabbitMqUri());

        return Response.status(Response.Status.OK).build();
    }
}
