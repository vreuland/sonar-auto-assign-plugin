package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.DryRunIncompatible;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link org.sonar.api.batch.PostJob} that sends a "issue change" notification
 * for new issues that have been auto-assigned by {@link IssueAutoAssignDecorator}
 * <p>
 * This post job targets only new issues. Indeed, change notification for old issues that are auto assigned are already
 * handled by the standard <code>SendIssueNotificationsPostJob</code>
 * </p>
 *
 * @author Vincent Reuland
 */
@DryRunIncompatible
public class SendAutoAssignedNewIssueNotificationPostJob implements PostJob {

  private static final Logger logger = LoggerFactory.getLogger(SendAutoAssignedNewIssueNotificationPostJob.class);
  private final IssueCache issueCache;
  private final AutoAssignedNewIssueNotificationFactory notificationFactory;
  private final NotificationManager notificationManager;
  private final boolean enabled;

  public SendAutoAssignedNewIssueNotificationPostJob(Settings settings, IssueCache issueCache, AutoAssignedNewIssueNotificationFactory notificationFactory, NotificationManager notificationManager) {
    this.issueCache = issueCache;
    this.notificationFactory = notificationFactory;
    this.notificationManager = notificationManager;
    this.enabled = settings.getBoolean(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED);
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    if (enabled) {
      sendNotifications(project);
    }
  }

  private void sendNotifications(Project project) {
    IssueChangeContext context = IssueChangeContext.createScan(project.getAnalysisDate());
    Map<DefaultIssue, Rule> newAssignedIssues = new LinkedHashMap<DefaultIssue, Rule>();
    for (DefaultIssue issue : issueCache.all()) {
      if (issue.isNew() && issue.assignee() != null) {
        logger.debug("Sending notification for issue [{}] to user [{}]", issue.key(), issue.assignee());
        notificationManager.scheduleForSending(notificationFactory.create(project, issue));
      }
    }
  }
}
