package org.example.Repository;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Order;
import org.example.Domain.OrderStatus;
import org.example.Entity.OrderRecord;

import java.util.List;

public class OrderRepo {
    private final EntityManagerFactory emf;

    public OrderRepo(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void save(Order order){
        emf.runInTransaction(em->em.persist(new OrderRecord(order)));
    }

    public void recordKitchen(int orderId, OrderStatus status, String chef){
        emf.runInTransaction(em->em.find(OrderRecord.class, orderId).kitchen(status, chef));
    }
    public void recordService(int orderId, OrderStatus status, String waiter){
        emf.runInTransaction(em->em.find(OrderRecord.class, orderId).service(status, waiter));
    }
    public List<OrderRecord> findAll(){
        return emf.callInTransaction(em->em.createQuery("SELECT o FROM OrderRecord o ORDER BY o.orderId", OrderRecord.class).getResultList());
    }

    /** First free order id: continues after the highest id already stored, or 101 on an empty table. */
    public int nextOrderId(){
        // MAX() on an empty table returns a single null row, so read the list instead of a stream
        List<Integer> result = emf.callInTransaction(em->em.createQuery("SELECT MAX(o.orderId) FROM OrderRecord o", Integer.class)
                .getResultList());
        Integer highest = result.isEmpty() ? null : result.get(0);
        return highest == null ? 101 : highest + 1;
    }
}
