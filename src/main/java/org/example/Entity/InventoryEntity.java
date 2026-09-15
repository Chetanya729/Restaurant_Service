package org.example.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.example.Domain.MenuItems;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @Column(name = "item")
    private String item;

    @Column(name = "quantity")
    private int quantity;

    public InventoryEntity(MenuItems item, int quantity) {
        this.item = item.name();
        this.quantity = quantity;
    }

    protected InventoryEntity(){

    }
    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
