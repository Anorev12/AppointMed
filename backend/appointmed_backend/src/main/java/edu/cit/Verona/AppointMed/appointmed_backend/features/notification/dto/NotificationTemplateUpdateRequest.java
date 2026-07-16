package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto;

public class NotificationTemplateUpdateRequest {

    private String subjectTemplate;
    private String customMessage;

    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
}
