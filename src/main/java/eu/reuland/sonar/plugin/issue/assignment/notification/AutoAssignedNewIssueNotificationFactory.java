package eu.reuland.sonar.plugin.issue.assignment.notification;

import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Locale;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.*;

/**
 * Creates <i>auto-assigned-new-issue</i> {@link Notification}s
 *
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationFactory implements BatchExtension, ServerExtension {

  public static final String NOTIFICATION_TYPE_KEY = "auto-assigned-new-issue";

  private final RuleFinder ruleFinder;
  private final RuleI18nManager ruleI18n;

  public AutoAssignedNewIssueNotificationFactory(RuleFinder ruleFinder, RuleI18nManager ruleI18n) {
    this.ruleFinder = ruleFinder;
    this.ruleI18n = ruleI18n;
  }

  public Notification create(Project project, Issue issue) {
    return new Notification(NOTIFICATION_TYPE_KEY)
        .setDefaultMessage(String.format("A new issue on %s has been assigned to you: %s",
            project.getName(), issue.key()))
        .setFieldValue(PROJECT_NAME.name(), project.getName())
        .setFieldValue(PROJECT_KEY.name(), project.key())
        .setFieldValue(COMPONENT_KEY.name(), issue.componentKey())
        .setFieldValue(ISSUE_KEY.name(), issue.key())
        .setFieldValue(ASSIGNEE.name(), issue.assignee())
        .setFieldValue(SEVERITY.name(), issue.severity())
        .setFieldValue(RULE_NAME.name(), ruleName(issue.ruleKey()))
        .setFieldValue(MESSAGE.name(), issue.message());
  }

  private String ruleName(RuleKey ruleKey) {
    Rule rule = ruleFinder.findByKey(ruleKey);

    String name = ruleI18n.getName(rule, Locale.ENGLISH);

    if (name == null) {
      name = rule.getName();
    }
    return name;
  }

}
