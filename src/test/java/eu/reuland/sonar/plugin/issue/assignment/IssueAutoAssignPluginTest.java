package eu.reuland.sonar.plugin.issue.assignment;

import org.fest.assertions.Assert;
import org.fest.assertions.Assertions;
import org.junit.Test;

/**
 * @author Vincent Reuland
 */
public class IssueAutoAssignPluginTest {
  @Test
  public void getExtensions() throws Exception {
    Assertions.assertThat(new IssueAutoAssignPlugin().getExtensions()).hasSize(4);
  }
}
