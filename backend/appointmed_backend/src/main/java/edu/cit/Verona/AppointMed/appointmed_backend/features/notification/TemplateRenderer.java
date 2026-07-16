package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merges {{placeholder}} tokens into an admin-edited subject line template.
 * Deliberately simple (no conditionals/loops) — admins are editing a single
 * line of plain text via a form field, not authoring a full template
 * language. Unknown/misspelled placeholders are left as literal text
 * (e.g. "{{typo}}") rather than throwing, so a bad edit degrades gracefully
 * instead of breaking every outgoing notification.
 */
@Component
public class TemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    public String render(String template, Map<String, String> vars) {
        if (template == null) return "";
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.getOrDefault(key, matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}