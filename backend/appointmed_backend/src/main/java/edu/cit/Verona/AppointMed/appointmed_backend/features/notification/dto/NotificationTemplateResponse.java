package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto;

public class NotificationTemplateResponse {

    private String type;
    private String label;
    private String subjectTemplate;
    private String customMessage;
    private String availablePlaceholders;

    public NotificationTemplateResponse() {}

    public NotificationTemplateResponse(String type, String label, String subjectTemplate,
                                         String customMessage, String availablePlaceholders) {
        this.type = type;
        this.label = label;
        this.subjectTemplate = subjectTemplate;
        this.customMessage = customMessage;
        this.availablePlaceholders = availablePlaceholders;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }

    public String getAvailablePlaceholders() { return availablePlaceholders; }
    public void setAvailablePlaceholders(String availablePlaceholders) { this.availablePlaceholders = availablePlaceholders; }
}
