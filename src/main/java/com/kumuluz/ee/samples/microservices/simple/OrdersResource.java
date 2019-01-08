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


import com.kumuluz.ee.discovery.annotations.DiscoverService;
import org.json.JSONObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import com.kumuluz.ee.common.runtime.EeRuntime;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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


    private final static String QUEUE_NAME = "orders";

    @Inject
    @DiscoverService(value = "emailing-service", environment = "dev", version = "*")
    private Optional<WebTarget> e_target;

    @Inject
    @DiscoverService(value = "journaling-service", environment = "dev", version = "*")
    private Optional<WebTarget> j_target;



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



        //Send e-mail
        JSONObject email = new JSONObject();

        if (e_target.isPresent()) {
            WebTarget service = e_target.get().path("/sendEmail");
            email.put("from","dmvoicestudios@gmail.com");
            email.put("to","dmvoicestudios@gmail.com");
            email.put("subject","order completed!");
            email.put("body","your order has been completed mr_customer");

            try {

                service.request().post(Entity.json(email.toString()));
            } catch (ProcessingException e) {
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();

            }
        }


        //Make new journal entry
        JSONObject entry = new JSONObject();

        if (j_target.isPresent()) {
            WebTarget service = j_target.get().path("/insertJournal");
            entry.put("id",1);
            entry.put("entryJSON",orderJSON);
            entry.put("entryDate",new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
            entry.put("entryService","Orders");
            entry.put("entryInstance_Id",EeRuntime.getInstance().getInstanceId());
            try {

                service.request().post(Entity.json(entry.toString()));
            } catch (ProcessingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


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
