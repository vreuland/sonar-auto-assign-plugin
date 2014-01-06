package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.issue.IssueCache;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Vincent Reuland
 */
public class SendAutoAssignedNewIssueNotificationPostJobTest {

  private Project project = mock(Project.class);
  private IssueCache issueCache = mock(IssueCache.class, RETURNS_MOCKS);
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private SensorContext sensorContext = mock(SensorContext.class);
  private Settings settings = new Settings(
      new PropertyDefinitions(IssueAutoAssignPlugin.propertyDefinitions()));
  private AutoAssignedNewIssueNotificationFactory notificationFactory = mock(AutoAssignedNewIssueNotificationFactory.class);


  @Before
  public void enablePlugin() {
    // The PostJob is disabled by default. => Enable it for tests
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED, true);
  }

  @Test
  public void shouldSendNotificationIfNewIssueHasBeenAssigned() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    Rule rule = new Rule("squid", "AvoidCycles");
    DefaultIssue toBeNotifiedIssue = new DefaultIssue().setNew(true).setAssignee("defaultAssignee").setRuleKey(ruleKey);
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-12-31"));
    when(issueCache.all()).thenReturn(Arrays.asList(
        new DefaultIssue().setNew(true).setRuleKey(ruleKey), // no assignee (should not be notified)
        toBeNotifiedIssue,
        new DefaultIssue().setNew(false).setAssignee("defaultAssignee") // not a new issue (should not be notified)
    ));

    SendAutoAssignedNewIssueNotificationPostJob job = new SendAutoAssignedNewIssueNotificationPostJob(settings, issueCache, notificationFactory, notificationManager);
    job.executeOn(project, sensorContext);

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    Notification generatedNotification = verify(notificationFactory).create(eq(project), eq(toBeNotifiedIssue));
    verify(notificationManager).scheduleForSending(notificationCaptor.capture());
    assertThat(notificationCaptor.getAllValues().size()).isEqualTo(1);
    assertThat(notificationCaptor.getValue()).isEqualTo(generatedNotification);

  }

  @Test
  public void doNothingWhenNotEnabled() throws Exception {
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED, false);
    SendAutoAssignedNewIssueNotificationPostJob job = new SendAutoAssignedNewIssueNotificationPostJob(settings, issueCache, notificationFactory, notificationManager);
    job.executeOn(project, sensorContext);
    verifyZeroInteractions(project, sensorContext, issueCache, notificationFactory, notificationManager);
  }
}