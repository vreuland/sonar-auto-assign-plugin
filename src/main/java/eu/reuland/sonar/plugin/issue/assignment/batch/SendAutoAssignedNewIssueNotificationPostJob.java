package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.DryRunIncompatible;
import org.sonar.core.issue.IssueNotifications;

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


  private final IssueCache issueCache;
  private final IssueNotifications notifications;
  private final RuleFinder ruleFinder;
  private final boolean enabled;

  public SendAutoAssignedNewIssueNotificationPostJob(Settings settings, IssueCache issueCache, IssueNotifications notifications, RuleFinder ruleFinder) {
    this.issueCache = issueCache;
    this.notifications = notifications;
    this.ruleFinder = ruleFinder;
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
        Rule rule = ruleFinder.findByKey(issue.ruleKey());
        if (rule != null) {
          newAssignedIssues.put(issue, rule);
        }
      }
    }
    if (!newAssignedIssues.isEmpty()) {
      notifications.sendChanges(newAssignedIssues, context, project, null);
    }
  }
}
