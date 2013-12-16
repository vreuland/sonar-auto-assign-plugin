package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.user.User;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.java.api.JavaClass;
import org.sonar.java.api.JavaMethod;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Vincent Reuland
 */
public class IssueAutoAssignDecoratorTest {

  private IssueAutoAssignDecorator decorator;
  private IssueCache issueCache = mock(IssueCache.class, RETURNS_MOCKS);
  private Project project = mock(Project.class);
  private IssueUpdater updater = mock(IssueUpdater.class);
  private ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
  private org.sonar.core.user.UserDao userDao = mock(UserDao.class);
  private Settings settings = new Settings(
      new PropertyDefinitions(IssueAutoAssignPlugin.propertyDefinitions()));

  @Before
  public void enablePlugin() {
    // The plugin is disabled by default. => Enabled it for tests
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED, true);
  }

  private Decorator decorator() {
    return new IssueAutoAssignDecorator(settings, project, issueCache, updater, perspectives, userDao);
  }

  @Test
  public void shouldBeExecutedOnProject() {
    Project project = mock(Project.class);
    assertThat(decorator().shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void doNothingWhenNotEnabled() {
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED, false);
    final Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    final DecoratorContext context = mock(DecoratorContext.class);

    decorator().decorate(file, context);
    verifyZeroInteractions(issueCache, updater, perspectives, userDao, context);
  }

  @Test
  public void shouldNotBeExecutedOnClasses() { // As classes are not "issueable"
    final DecoratorContext context = mock(DecoratorContext.class);
    decorator().decorate(JavaClass.create("org.foo.bar"), context);
    verifyZeroInteractions(issueCache, updater, userDao, context);
  }

  @Test
  public void shouldNotBeExecutedOnMethods() { // As methods are not "issueable"
    final DecoratorContext context = mock(DecoratorContext.class);
    decorator().decorate(JavaMethod.createRef(JavaClass.create("org.foo.bar"), "init"), context);
    verifyZeroInteractions(issueCache, updater, userDao, context);
  }

  @Test
  public void shouldExecuteOnFileWithNewIssues() {
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    DefaultIssue issue = new DefaultIssue().setKey("issueKey").setNew(true).setLine(2);
    Issuable issuable = mock(Issuable.class);
    Measure measure = mock(Measure.class);
    UserDto userDto = new UserDto().setLogin("loginB").setName("userB");

    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList(issue));
    when(context.getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(measure);
    when(measure.getData()).thenReturn("1=loginA;2=loginB;3=loginC");
    when(userDao.selectActiveUserByLogin("loginB")).thenReturn(userDto);

    decorator().decorate(file, context);

    ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
    verify(updater).assign(eq(issue), argument.capture(), any(IssueChangeContext.class));
    assertThat(argument.getValue().name()).isEqualTo("userB");

    verify(issueCache).put(issue);
  }

  @Test
  public void shouldNotExecuteOnResolvedIssue() {
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    Issuable issuable = mock(Issuable.class);
    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList()); // No unresolved issues returned

    decorator().decorate(file, context);

    verifyZeroInteractions(issueCache, updater);
  }

  @Test
  public void shouldNotExecuteOnAlreadyAssignedIssue() {
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    Issuable issuable = mock(Issuable.class);
    DefaultIssue issue = new DefaultIssue().setKey("issueKey").setNew(true).setAssignee("assignee").setLine(2);

    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList(issue)); // No unresolved issues returned

    decorator().decorate(file, context);

    verifyZeroInteractions(context, issueCache, updater);
  }

  @Test
  public void useDefaultAssignee() {
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE, "defaultAssignee");
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    DefaultIssue issue = new DefaultIssue().setKey("issueKey").setNew(true).setLine(2);
    Issuable issuable = mock(Issuable.class);
    Measure measure = mock(Measure.class);
    UserDto userDto = new UserDto().setLogin("defaultAssignee").setName("username");

    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList(issue));
    when(context.getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(null);
    when(userDao.selectActiveUserByLogin("defaultAssignee")).thenReturn(userDto);

    decorator().decorate(file, context);

    ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
    verify(updater).assign(eq(issue), argument.capture(), any(IssueChangeContext.class));
    assertThat(argument.getValue().name()).isEqualTo("username");

    verify(issueCache).put(issue);
  }

  @Test
  public void shouldNotExecuteOnOldIssueByDefault() {
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    DefaultIssue issue = new DefaultIssue().setKey("issueKey").setNew(false).setLine(2);
    Issuable issuable = mock(Issuable.class);


    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList(issue));

    decorator().decorate(file, context);

    verifyZeroInteractions(context, issueCache, updater);
  }

  @Test
  public void setNewIssueOnlyParameterSetToFalse() {
    settings.setProperty(IssueAutoAssignPlugin.PROPERTY_NEW_ISSUES_ONLY, false);
    final DecoratorContext context = mock(DecoratorContext.class);
    Resource file = new File("Resource.java").setEffectiveKey("effectivekey").setId(1);
    DefaultIssue issue = new DefaultIssue().setKey("issueKey").setNew(false).setLine(2);
    Issuable issuable = mock(Issuable.class);

    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.issues()).thenReturn(Arrays.<Issue>asList(issue));

    decorator().decorate(file, context);

    verify(context).getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE);
  }

}
