package eu.reuland.sonar.plugin.issue.assignment;

import com.google.common.collect.ImmutableList;
import eu.reuland.sonar.plugin.issue.assignment.batch.IssueAutoAssignDecorator;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link SonarPlugin} that automatically assigns unresolved (and unassigned) issues based on the SCM
 * blame information
 *
 * @author Vincent Reuland
 */
public final class IssueAutoAssignPlugin extends SonarPlugin {

  public static final String PROPERTY_PLUGIN_ENABLED = "sonar.plugin.auto.assign.enabled";
  public static final String PROPERTY_NEW_ISSUES_ONLY = "sonar.plugin.auto.assign.new_issues_only";
  public static final String PROPERTY_DEFAULT_ASSIGNEE = "sonar.plugin.auto.assign.default_assignee";

  public static List<PropertyDefinition> propertyDefinitions() {
    return Arrays.asList(
        PropertyDefinition.builder(PROPERTY_PLUGIN_ENABLED)
            .name("Enable")
            .description("Activate the plugin")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue("false")
            .index(0)
            .build(),
        PropertyDefinition.builder(PROPERTY_NEW_ISSUES_ONLY)
            .name("Only new issues")
            .description("Determine whether only news issues must be automatically assigned or all unresolved " +
                "and unassigned issues")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue("true")
            .index(1)
            .build(),
        PropertyDefinition.builder(PROPERTY_DEFAULT_ASSIGNEE)
            .name("Default assignee")
            .description("The login of the assignee to use if no one can be automatically determined by the plugin " +
                "through SCM blame. If empty the issue will remain unassigned")
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.STRING)
            .index(2)
            .build()
    );
  }


  public List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.add(IssueAutoAssignDecorator.class);
    extensions.addAll(propertyDefinitions());

    return extensions.build();
  }
}
