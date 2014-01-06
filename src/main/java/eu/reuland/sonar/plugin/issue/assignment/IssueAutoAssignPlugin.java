package eu.reuland.sonar.plugin.issue.assignment;

import com.google.common.collect.ImmutableList;
import eu.reuland.sonar.plugin.issue.assignment.batch.IssueAutoAssignDecorator;
import eu.reuland.sonar.plugin.issue.assignment.batch.SendAutoAssignedNewIssueNotificationPostJob;
import eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationDispatcher;
import eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationEmailTemplate;
import eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link SonarPlugin} that defines:
 * <ul>
 * <li>
 * a {@link org.sonar.api.batch.Decorator} that automatically assigns unresolved
 * (and unassigned) issues based on the SCM blame information,
 * </li>
 * <li>
 * a {@link org.sonar.api.batch.PostJob} that sends a issue change notification for new issues
 * that have been automatically assigned.
 * </li>
 * </ul>
 *
 * @author Vincent Reuland
 */
public final class IssueAutoAssignPlugin extends SonarPlugin {

  public static final String PROPERTY_PLUGIN_ENABLED = "sonar.autoassign.enabled";
  public static final String PROPERTY_NEW_ISSUES_ONLY = "sonar.autoassign.new_issues_only";
  public static final String PROPERTY_DEFAULT_ASSIGNEE = "sonar.autoassign.default_assignee";

  public static List<PropertyDefinition> propertyDefinitions() {
    return Arrays.asList(
        PropertyDefinition.builder(PROPERTY_PLUGIN_ENABLED)
            .name("Enable")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue("false")
            .index(0)
            .build(),
        PropertyDefinition.builder(PROPERTY_NEW_ISSUES_ONLY)
            .name("Only new issues")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue("true")
            .index(1)
            .build(),
        PropertyDefinition.builder(PROPERTY_DEFAULT_ASSIGNEE)
            .name("Default assignee")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.STRING)
            .index(2)
            .build()
    );
  }


  public List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.add(IssueAutoAssignDecorator.class);
    extensions.add(AutoAssignedNewIssueNotificationDispatcher.class);
    extensions.add(AutoAssignedNewIssueNotificationDispatcher.newMetadata());
    extensions.add(AutoAssignedNewIssueNotificationFactory.class);
    extensions.add(AutoAssignedNewIssueNotificationEmailTemplate.class);
    extensions.add(SendAutoAssignedNewIssueNotificationPostJob.class);
    extensions.addAll(propertyDefinitions());

    return extensions.build();
  }
}
