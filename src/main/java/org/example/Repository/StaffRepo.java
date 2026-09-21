package org.example.Repository;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Role;
import org.example.Entity.StaffMember;

import java.util.List;

/** Keeps hired staff in the database so they survive a restart. */
public class StaffRepo {

    private final EntityManagerFactory emf;

    public StaffRepo(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<String> findActiveNames(Role role) {
        return emf.callInTransaction(em -> em.createQuery(
                        "SELECT s.name FROM StaffMember s WHERE s.role = :role AND s.active = true ORDER BY s.id",
                        String.class)
                .setParameter("role", role)
                .getResultList());
    }

    public List<StaffMember> findAll() {
        return emf.callInTransaction(em -> em.createQuery(
                "SELECT s FROM StaffMember s ORDER BY s.id", StaffMember.class).getResultList());
    }

    /** Adds the staff member, or reactivates someone who left earlier. */
    public void hire(String name, Role role) {
        emf.runInTransaction(em -> {
            StaffMember existing = findByName(em, name);
            if (existing == null) {
                em.persist(new StaffMember(name, role));
            } else {
                existing.rehire(role);
            }
        });
    }

    /** Marks the staff member inactive; the row stays so old orders keep their history. */
    public void fire(String name, Role role) {
        emf.runInTransaction(em -> {
            StaffMember existing = findByName(em, name);
            if (existing != null) {
                existing.leave();
            }
        });
    }

    private static StaffMember findByName(jakarta.persistence.EntityManager em, String name) {
        return em.createQuery("SELECT s FROM StaffMember s WHERE s.name = :name", StaffMember.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
