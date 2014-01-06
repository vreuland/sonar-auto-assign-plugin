package eu.reuland.sonar.plugin.issue.assignment.notification;

/**
 * Enumeration of available {@link org.sonar.api.notifications.Notification} fields
 * names in AutoAssignedNewIssue notifications
 * <p>
 * To be used as simple string constants, enum values names corresponding to fields names
 * </p>
 *
 * @author Vincent Reuland
 * @see org.sonar.api.notifications.Notification#getFieldValue(String)
 */
public enum AutoAssignedNewIssueNotificationField {
  PROJECT_KEY,
  PROJECT_NAME,
  COMPONENT_KEY,
  ISSUE_KEY,
  ASSIGNEE,
  SEVERITY,
  RULE_NAME,
  MESSAGE
}
