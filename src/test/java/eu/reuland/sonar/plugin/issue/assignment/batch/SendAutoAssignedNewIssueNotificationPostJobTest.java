package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.Component;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssueNotifications;

import java.util.Arrays;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

/**
 * @author Vincent Reuland
 */
public class SendAutoAssignedNewIssueNotificationPostJobTest {

    private Project project = mock(Project.class);
    private IssueCache issueCache = mock(IssueCache.class, RETURNS_MOCKS);
    private IssueNotifications notifications = mock(IssueNotifications.class);
    private RuleFinder ruleFinder = mock(RuleFinder.class);
    private SensorContext sensorContext = mock(SensorContext.class);
    private Settings settings = new Settings(
            new PropertyDefinitions(IssueAutoAssignPlugin.propertyDefinitions()));

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
        when(ruleFinder.findByKey(ruleKey)).thenReturn(rule);
        when(issueCache.all()).thenReturn(Arrays.asList(
                new DefaultIssue().setNew(true).setRuleKey(ruleKey), // no assignee (should not be notified)
                toBeNotifiedIssue,
                new DefaultIssue().setNew(false).setAssignee("defaultAssignee") // not a new issue (should not be notified)
        ));

        SendAutoAssignedNewIssueNotificationPostJob job = new SendAutoAssignedNewIssueNotificationPostJob(settings, issueCache, notifications, ruleFinder);
        job.executeOn(project, sensorContext);

        ArgumentCaptor<Map> issueMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notifications).sendChanges(issueMapCaptor.capture(), any(IssueChangeContext.class), eq(project), (Component) isNull());
        assertThat(issueMapCaptor.getValue().size()).isEqualTo(1);
        assertThat(issueMapCaptor.getValue().containsKey(toBeNotifiedIssue)).isTrue();
    }

    @Test
    public void doNothingWhenNotEnabled() throws Exception {
        settings.setProperty(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED, false);
        SendAutoAssignedNewIssueNotificationPostJob job = new SendAutoAssignedNewIssueNotificationPostJob(settings, issueCache, notifications, ruleFinder);
        job.executeOn(project, sensorContext);
        verifyZeroInteractions(project, sensorContext, issueCache, notifications, ruleFinder);
    }
}