package edu.cit.Verona.AppointMed.appointmed_backend.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 
/**
 * NFR-005: "All user passwords shall be stored ... using BCrypt with a
 * minimum cost factor of 12."
 *
 * This is the only place the cost factor is set. PatientService,
 * DoctorService, and AdminService never construct their own encoder —
 * they all go through PasswordVerifier, which wraps this bean.
 */
@Configuration
public class PasswordConfig {
 
    private static final int BCRYPT_COST_FACTOR = 12;
 
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_COST_FACTOR);
    }
}