package com.kumuluz.ee.samples.microservices.simple;

import com.kumuluz.ee.samples.microservices.simple.Models.Order;

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
import java.util.Date;
import java.util.List;

@Path("/orders")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrdersResource {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private OrdersProperties ordersProperties;

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
    public Response placeOrder(JSONObject orderJSON) {

        /*if (b == null || b.getId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Response bookResponse = ClientBuilder.newClient()
                .target(ordersProperties.getCatalogUrl()).path("books").path(b.getId().toString()).request().get();

        if (!bookResponse.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Order o = new Order();
        o.setBook(bookResponse.readEntity(Book.class));
        o.setOrderDate(new Date());

        em.getTransaction().begin();

        em.persist(o);

        em.getTransaction().commit();*/

        Order o = new Order();
        o.setOrderDate(new Date());

        return Response.status(Response.Status.CREATED).entity(o).build();
    }
}
