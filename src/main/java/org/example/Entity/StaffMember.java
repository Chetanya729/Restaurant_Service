package org.example.Entity;

import jakarta.persistence.*;
import org.example.Domain.Role;

@Entity
@Table(name = "staff")
public class StaffMember {

    // no natural id for staff, so the database generates one
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    protected StaffMember() {
    }

    public StaffMember(String name, Role role) {
        this.name = name;
        this.role = role;
        this.active = true;
    }

    public void rehire(Role role) {
        this.role = role;
        this.active = true;
    }

    public void leave() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
