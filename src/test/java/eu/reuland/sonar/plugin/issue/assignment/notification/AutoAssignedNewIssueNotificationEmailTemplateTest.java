package eu.reuland.sonar.plugin.issue.assignment.notification;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.user.UserFinder;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.test.TestUtils;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationEmailTemplateTest {

  private UserFinder userFinder = mock(UserFinder.class);
  private AutoAssignedNewIssueNotificationEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://localhost:9000");
    template = new AutoAssignedNewIssueNotificationEmailTemplate(settings);
  }

  @Test
  public void shouldIgnoreNonAutoAssignedNewIssueNotification() {
    Notification notification = new Notification("other");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void formatEmail() throws Exception {
    Notification notification = new Notification(AutoAssignedNewIssueNotificationFactory.NOTIFICATION_TYPE_KEY)
        .setFieldValue(PROJECT_NAME.name(), "Struts")
        .setFieldValue(PROJECT_KEY.name(), "org.apache:struts")
        .setFieldValue(COMPONENT_KEY.name(), "org.apache:struts:org.apache.struts.Action")
        .setFieldValue(ISSUE_KEY.name(), "ABCDE")
        .setFieldValue(ASSIGNEE.name(), "defaultAssignee")
        .setFieldValue(SEVERITY.name(), "MAJOR")
        .setFieldValue(RULE_NAME.name(), "Avoid Cycles")
        .setFieldValue(MESSAGE.name(), "Has 3 cycles");

    EmailMessage email = template.format(notification);
    assertThat(email.getMessageId()).isEqualTo("auto-assigned-new-issue/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, a new issue has been assigned to you: #ABCDE");

    String expectedMessage = TestUtils.getResourceContent("/test/sample/notification/email_expected_body.txt");
    assertThat(email.getMessage()).isEqualTo(expectedMessage);
    assertThat(email.getFrom()).isNull();
  }


}
