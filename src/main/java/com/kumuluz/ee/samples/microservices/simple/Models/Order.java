package com.kumuluz.ee.samples.microservices.simple.Models;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "orders")
@NamedQuery(name = "BookOrder.findAll", query = "SELECT o FROM Order o")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;

    private String orderJSON;
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderJSON() {
        return orderJSON;
    }

    public void setOrderJSON(String orderJSON) {
        this.orderJSON = orderJSON;
    }
}
