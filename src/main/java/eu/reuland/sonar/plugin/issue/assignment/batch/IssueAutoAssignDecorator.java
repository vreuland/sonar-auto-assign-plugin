package eu.reuland.sonar.plugin.issue.assignment.batch;

import eu.reuland.sonar.plugin.issue.assignment.IssueAutoAssignPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.user.User;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link Decorator} that assign issues (related to decorated resource) based on the SCM blame information
 *
 * @author Vincent Reuland
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public class IssueAutoAssignDecorator implements Decorator {

  private static final Logger logger = LoggerFactory.getLogger(IssueAutoAssignDecorator.class);
  private static final Map<Integer, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<Integer, String>());
  private final Settings settings;
  private final IssueCache issueCache;
  private final IssueUpdater issueUpdater;
  private final IssueChangeContext changeContext;
  private final ResourcePerspectives perspectives;
  private final UserDao userDao;
  private final boolean enabled;

  public IssueAutoAssignDecorator(Settings settings, Project project, IssueCache issueCache, IssueUpdater issueUpdater, ResourcePerspectives perspectives, UserDao userDao) {
    this.settings = settings;
    this.issueCache = issueCache;
    this.issueUpdater = issueUpdater;
    this.changeContext = IssueChangeContext.createScan(project.getAnalysisDate());
    this.perspectives = perspectives;
    this.userDao = userDao;
    this.enabled = settings.getBoolean(IssueAutoAssignPlugin.PROPERTY_PLUGIN_ENABLED);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!enabled) { return; }

    logger.trace("Decorating resource [{}]", resource.getKey());

    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable == null) {
      logger.trace("Resource [{}] is not issuable", resource.getKey());
      return;
    }

    List<Issue> unresolvedIssues = issuable.issues();
    if (unresolvedIssues == null) {
      logger.debug("No unresolved issues found for resource [{}]", resource.getKey());
      return;
    }

    Map<Integer, String> authorsByLine = null;
    for (Issue issue : unresolvedIssues) {
      logger.debug("Treating unresolved issue [{}]: isNew = [{}], line = [{}], assignee = [{}]",
          issue.key(), issue.isNew(), issue.line(), issue.assignee());

      if (!isCandidateIssue(issue)) {
        logger.debug("Issue [{}] is not a candidate for auto assignment", issue.key());
        continue;
      }

      if (authorsByLine == null) {
        // Load authors by line for the current resource. Should be done only once per resource
        authorsByLine = getAuthorsByLineFromScm(context);
      }

      User autoAssignee = getAutoAssignee(issue, authorsByLine);
      if (autoAssignee != null) {
        logger.info("Assigning issue [{}] to user [{}]", issue.key(), autoAssignee.login());
        assignIssue(issue, autoAssignee);
      } else {
        logger.info("Leaving the issue [{}] unassigned", issue.key());
      }
    }
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  private boolean isCandidateIssue(Issue issue) {
    return issue.assignee() == null &&
        (issue.isNew() || !settings.getBoolean(IssueAutoAssignPlugin.PROPERTY_NEW_ISSUES_ONLY));
  }

  private User getAutoAssignee(Issue issue, Map<Integer, String> authorsByLine) {
    String scmLoginName = null;
    if (issue.line() != null || authorsByLine.get(issue.line()) != null) {
      scmLoginName = authorsByLine.get(issue.line());
    }

    User autoAssignee = null;
    if (scmLoginName == null) {
      logger.debug("Cannot detect automatically the login of the assignee from SCM blame for issue [{}]", issue.key());
      autoAssignee = getDefaultAssigneeIfAny();
      if (autoAssignee != null) {
        logger.debug("Using default assignee [{}] for issue [{}]", autoAssignee.login(), issue.key());
      }
    } else {
      autoAssignee = getUserFromLoginName(scmLoginName);
    }
    return autoAssignee;
  }

  private User getDefaultAssigneeIfAny() {
    User defaultAssignee = null;
    String defaultAssigneeLogin = settings.getString(IssueAutoAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE);
    if (defaultAssigneeLogin != null && defaultAssigneeLogin.trim().length() > 0) {
      defaultAssignee = getUserFromLoginName(defaultAssigneeLogin);
      if (defaultAssignee == null) {
        logger.error("The specified login [{}] (for default assignation) doesn't correspond to an active user." +
            " Please correct the configuration", defaultAssigneeLogin);
      }
    }
    return defaultAssignee;
  }

  private User getUserFromLoginName(String login) {
    UserDto user = userDao.selectActiveUserByLogin(login);
    if (user == null) {
      logger.warn("Cannot find an active user with login name [{}]", login);
      return null;
    }
    return user.toUser();
  }

  private void assignIssue(Issue issue, User user) {
    issueUpdater.assign((DefaultIssue) issue, user, changeContext);
    issueCache.put((DefaultIssue) issue); // To be taken into account by the IssuePersister launched at the end of the scan
  }

  private Map<Integer, String> getAuthorsByLineFromScm(DecoratorContext context) {
    Measure measure = context.getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE);

    if (measure != null && measure.getData() != null && measure.getData().length() > 0) {
      logger.debug("Measure for metric [{}] data: [{}]", measure.getMetricKey(), measure.getData());
      return parseAuthorsByLineMeasure(measure.getData());
    } else {
      logger.debug("Cannot find measure for metric [{}]", CoreMetrics.SCM_AUTHORS_BY_LINE.getKey());
      return EMPTY_MAP;
    }
  }

  private Map<Integer, String> parseAuthorsByLineMeasure(String measureEntry) {
    Map<Integer, String> authorsByLine = new HashMap<Integer, String>();

    String[] lineNumberWithAuthorTable = measureEntry.split(";");
    for (String lineNumberWithAuthor : lineNumberWithAuthorTable) {
      int separatorIndex = lineNumberWithAuthor.indexOf('=');
      String lineNumber = lineNumberWithAuthor.substring(0, separatorIndex);
      String author = lineNumberWithAuthor.substring(separatorIndex + 1);
      authorsByLine.put(Integer.valueOf(lineNumber), author);
    }

    return authorsByLine;
  }


}
