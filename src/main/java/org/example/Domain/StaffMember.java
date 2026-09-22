package org.example.Domain;

import jakarta.persistence.*;

import javax.management.relation.Role;

@Entity
@Table(name = "Staff_members" )
public class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

}
