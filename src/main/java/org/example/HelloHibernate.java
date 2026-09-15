package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.example.Domain.MenuItems;
import org.example.Entity.InventoryEntity;
import org.example.Repository.Database;

public class HelloHibernate {
    public static void main(String[] args) {
        EntityManagerFactory emf = Database.open();

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try{
            tx.begin();
            em.persist(new InventoryEntity(MenuItems.BURGER,8));
            tx.commit();
        }catch (RuntimeException e){
            if(tx.isActive()){
                tx.rollback();
            }throw e;
        }finally {
            em.close();
        }
        emf.close();
    }
}
