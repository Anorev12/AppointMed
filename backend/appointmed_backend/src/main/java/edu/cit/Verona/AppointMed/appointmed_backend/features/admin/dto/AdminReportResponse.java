package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto;
import java.util.List;
import java.util.Map;
 
/**
 * FR-035: "The system shall allow administrators to view reports on
 * appointment statistics and system activity."
 *
 * Aggregated on the backend (AdminService.generateReport) so the numbers
 * are consistent regardless of which client — web or mobile — renders
 * them, rather than each frontend re-deriving stats from raw list
 * endpoints (which is what the admin Overview tab did before this).
 */
public class AdminReportResponse {
 
    private long totalPatients;
    private long totalDoctors;
    private long totalAdmins;
 
    private long totalAppointments;
    private long appointmentsToday;
    private long appointmentsThisWeek;
 
    /** e.g. { "CONFIRMED": 12, "CANCELLED": 3, "COMPLETED": 40 } */
    private Map<String, Long> appointmentsByStatus;
 
    /** Doctors ranked by total appointment count, busiest first. */
    private List<DoctorLoad> topDoctorsByAppointments;
 
    /** e.g. { "SENT": 50, "LOGGED": 10, "UNDELIVERED": 1 } */
    private Map<String, Long> notificationsByStatus;
    private long totalNotifications;
 
    public AdminReportResponse() {}
 
    public AdminReportResponse(long totalPatients, long totalDoctors, long totalAdmins,
                                long totalAppointments, long appointmentsToday, long appointmentsThisWeek,
                                Map<String, Long> appointmentsByStatus, List<DoctorLoad> topDoctorsByAppointments,
                                Map<String, Long> notificationsByStatus, long totalNotifications) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalAdmins = totalAdmins;
        this.totalAppointments = totalAppointments;
        this.appointmentsToday = appointmentsToday;
        this.appointmentsThisWeek = appointmentsThisWeek;
        this.appointmentsByStatus = appointmentsByStatus;
        this.topDoctorsByAppointments = topDoctorsByAppointments;
        this.notificationsByStatus = notificationsByStatus;
        this.totalNotifications = totalNotifications;
    }
 
    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }
 
    public long getTotalDoctors() { return totalDoctors; }
    public void setTotalDoctors(long totalDoctors) { this.totalDoctors = totalDoctors; }
 
    public long getTotalAdmins() { return totalAdmins; }
    public void setTotalAdmins(long totalAdmins) { this.totalAdmins = totalAdmins; }
 
    public long getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(long totalAppointments) { this.totalAppointments = totalAppointments; }
 
    public long getAppointmentsToday() { return appointmentsToday; }
    public void setAppointmentsToday(long appointmentsToday) { this.appointmentsToday = appointmentsToday; }
 
    public long getAppointmentsThisWeek() { return appointmentsThisWeek; }
    public void setAppointmentsThisWeek(long appointmentsThisWeek) { this.appointmentsThisWeek = appointmentsThisWeek; }
 
    public Map<String, Long> getAppointmentsByStatus() { return appointmentsByStatus; }
    public void setAppointmentsByStatus(Map<String, Long> appointmentsByStatus) { this.appointmentsByStatus = appointmentsByStatus; }
 
    public List<DoctorLoad> getTopDoctorsByAppointments() { return topDoctorsByAppointments; }
    public void setTopDoctorsByAppointments(List<DoctorLoad> topDoctorsByAppointments) { this.topDoctorsByAppointments = topDoctorsByAppointments; }
 
    public Map<String, Long> getNotificationsByStatus() { return notificationsByStatus; }
    public void setNotificationsByStatus(Map<String, Long> notificationsByStatus) { this.notificationsByStatus = notificationsByStatus; }
 
    public long getTotalNotifications() { return totalNotifications; }
    public void setTotalNotifications(long totalNotifications) { this.totalNotifications = totalNotifications; }
 
    /** One row of the "busiest doctors" table in the report. */
    public static class DoctorLoad {
        private Long doctorId;
        private String doctorName;
        private String specialization;
        private long appointmentCount;
 
        public DoctorLoad() {}
 
        public DoctorLoad(Long doctorId, String doctorName, String specialization, long appointmentCount) {
            this.doctorId = doctorId;
            this.doctorName = doctorName;
            this.specialization = specialization;
            this.appointmentCount = appointmentCount;
        }
 
        public Long getDoctorId() { return doctorId; }
        public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
 
        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
 
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
 
        public long getAppointmentCount() { return appointmentCount; }
        public void setAppointmentCount(long appointmentCount) { this.appointmentCount = appointmentCount; }
    }
}
 