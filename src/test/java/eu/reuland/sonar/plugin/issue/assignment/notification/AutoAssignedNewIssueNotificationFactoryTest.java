package eu.reuland.sonar.plugin.issue.assignment.notification;

import org.junit.Test;
import org.sonar.api.i18n.RuleI18n;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Locale;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory.NOTIFICATION_TYPE_KEY;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationFactoryTest {

  RuleFinder ruleFinder = mock(RuleFinder.class);
  RuleI18nManager ruleI18n = mock(RuleI18nManager.class);
  Issue issue = mock(Issue.class);


  @Test
  public void testCreate() throws Exception {
    Project project = new Project("org.apache.struts");
    project.setName("Struts");
    RuleKey ruleKey = RuleKey.parse("squid:UselessImportCheck");
    Rule rule = Rule.create();

    when(issue.componentKey()).thenReturn("org.apache.struts::org.apache:struts:org.apache.struts.Action");
    when(issue.key()).thenReturn("ABCDE");
    when(issue.assignee()).thenReturn("defaultAssignee");
    when(issue.severity()).thenReturn("MINOR");
    when(issue.message()).thenReturn("Unused import message");
    when(issue.ruleKey()).thenReturn(ruleKey);
    when(ruleFinder.findByKey(ruleKey)).thenReturn(rule);
    when(ruleI18n.getName(rule, Locale.ENGLISH)).thenReturn("Useless imports should be removed");

    AutoAssignedNewIssueNotificationFactory factory = new AutoAssignedNewIssueNotificationFactory(ruleFinder, ruleI18n);
    Notification notification = factory.create(project, issue);

    assertThat(notification.getType()).isEqualTo(NOTIFICATION_TYPE_KEY);
    assertThat(notification.getDefaultMessage()).isNotEmpty();
    assertThat(notification.getFieldValue(PROJECT_NAME.name())).isEqualTo("Struts");
    assertThat(notification.getFieldValue(PROJECT_KEY.name())).isEqualTo("org.apache.struts");
    assertThat(notification.getFieldValue(COMPONENT_KEY.name())).isEqualTo("org.apache.struts::org.apache:struts:org.apache.struts.Action");
    assertThat(notification.getFieldValue(ISSUE_KEY.name())).isEqualTo("ABCDE");
    assertThat(notification.getFieldValue(ASSIGNEE.name())).isEqualTo("defaultAssignee");
    assertThat(notification.getFieldValue(SEVERITY.name())).isEqualTo("MINOR");
    assertThat(notification.getFieldValue(MESSAGE.name())).isEqualTo("Unused import message");
    assertThat(notification.getFieldValue(RULE_NAME.name())).isEqualTo("Useless imports should be removed");
  }
}
