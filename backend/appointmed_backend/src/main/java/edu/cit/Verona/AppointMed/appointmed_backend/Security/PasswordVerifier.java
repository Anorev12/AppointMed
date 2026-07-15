package edu.cit.Verona.AppointMed.appointmed_backend.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
 
/**
 * Single choke point for password hashing/verification across
 * PatientService, DoctorService, and AdminService (NFR-005).
 *
 * Migration note: accounts created before this change have a plaintext
 * password already saved in the database. Rather than requiring a manual
 * data migration or a password reset for every existing account,
 * matches() accepts a plaintext match ONLY when the stored value isn't a
 * BCrypt hash, and the calling service is expected to immediately
 * re-save the password as a proper hash after a successful login (see
 * upgradeIfNeeded()). Once that row has been hashed once, this fallback
 * never applies to it again. New accounts (registration, admin-created
 * doctor/patient/admin) are hashed on creation and never touch the
 * fallback path at all.
 */
@Component
public class PasswordVerifier {
 
    private final BCryptPasswordEncoder encoder;
 
    public PasswordVerifier(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }
 
    /** Hash a new/changed password before persisting it. */
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
 
    /**
     * @return true if rawPassword is correct, whether storedPassword is a
     *         BCrypt hash or (pre-migration rows only) plaintext.
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }
        // Pre-migration row: only reachable if it was never hashed yet.
        return storedPassword.equals(rawPassword);
    }
 
    /** BCrypt hashes always look like $2a$12$..., $2b$12$..., or $2y$12$... (60 chars total). */
    public boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}