package eu.reuland.sonar.plugin.issue.assignment.notification;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory.NOTIFICATION_TYPE_KEY;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.ISSUE_KEY;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.PROJECT_NAME;


/**
 * An {@link EmailTemplate} that formats AutoAssignedNewIssue notifications
 *
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationEmailTemplate extends EmailTemplate {

  private final EmailSettings settings;

  public AutoAssignedNewIssueNotificationEmailTemplate(EmailSettings settings) {
    this.settings = settings;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!NOTIFICATION_TYPE_KEY.equals(notification.getType())) {
      return null;
    }

    return new EmailMessage()
        .setMessageId(generateMessageId(notification))
        .setSubject(generateMessageSubject(notification))
        .setMessage(generateMessageBody(notification));
  }

  private String generateMessageId(Notification notification) {
    return NOTIFICATION_TYPE_KEY + "/" + notification.getFieldValue(ISSUE_KEY.name());
  }

  private String generateMessageSubject(Notification notification) {
    return String.format("%s, a new issue has been assigned to you: #%s",
        notification.getFieldValue(PROJECT_NAME.name()),
        notification.getFieldValue(ISSUE_KEY.name())
    );
  }

  private String generateMessageBody(Notification notification) {
    StringTemplate template = getEmailBodyTemplate();
    template.setAttribute("SETTINGS", settings);
    fillTemplateWithNotificationFields(template, notification);
    return template.toString();
  }

  private StringTemplate getEmailBodyTemplate() {
    StringTemplateGroup templateGroup = new StringTemplateGroup("templates");
    return templateGroup.getInstanceOf("templates/notification/email/auto-assigned-new-issue-template");
  }

  private void fillTemplateWithNotificationFields(StringTemplate template, Notification notification) {
    for (AutoAssignedNewIssueNotificationField field : AutoAssignedNewIssueNotificationField.values()) {
      template.setAttribute(field.name(), notification.getFieldValue(field.name()));
    }
  }


}
