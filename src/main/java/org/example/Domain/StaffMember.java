package org.example.Domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "Staff_members")
@Getter
public class StaffMember {

    public enum Role {
        CHEF,
        WAITER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    protected StaffMember() {

    }

    public StaffMember(String name, Role role) {
        this.name = name;
        this.role = role;
    }

    public void deactivate() {
        this.active = false;
    }
}
