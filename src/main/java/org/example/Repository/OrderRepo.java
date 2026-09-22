package org.example.Repository;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Order;
import org.example.Domain.OrderStatus;
import org.example.Entity.OrderRecord;

import java.sql.*;
import java.util.List;

public class OrderRepo {


    String url = "";
    String username="";
    String  password = "";

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
    public void createUser() throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES ('John Doe', 'john.doe@example.com')";
        Connection conn = DriverManager.getConnection(url,username, password);

        try
        {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()){
                System.out.println(rs.getString("name"));
            }
            statement.execute(sql);

        }catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
