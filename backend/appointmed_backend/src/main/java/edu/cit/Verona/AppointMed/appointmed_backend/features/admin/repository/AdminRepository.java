package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
}