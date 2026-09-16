package org.example.Repository;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.MenuItems;
import org.example.Entity.InventoryEntity;

import java.util.Map;

public class InventoryRepo {

    private static EntityManagerFactory emf;

    public InventoryRepo(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void seed(Map<MenuItems , Integer> stock){
        emf.runInTransaction(em -> stock.forEach((item, quantity) -> em.persist(new InventoryEntity(item, quantity))));
    }
    public boolean reserve(MenuItems menuItem, int quantity){
        int updated = emf.callInTransaction(em-> em.createQuery("UPDATE InventoryEntity i SET i.quantity = i.quantity - :quantity WHERE i.menuItem = :menuItem", InventoryEntity.class)
                .setParameter("quantity", quantity)
                .setParameter("menuItem", menuItem)
                .executeUpdate());
        return updated==1;
    }
    public int getStock(MenuItems menuItem){
        return emf.callInTransaction(em->em.find(InventoryEntity.class, menuItem.name()).getQuantity());
    }
}
