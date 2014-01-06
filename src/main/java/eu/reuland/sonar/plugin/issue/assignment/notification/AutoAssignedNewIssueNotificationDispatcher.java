package eu.reuland.sonar.plugin.issue.assignment.notification;

import com.google.common.collect.Multimap;
import org.picocontainer.annotations.Nullable;
import org.sonar.api.notifications.*;

import java.util.Collection;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.ASSIGNEE;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.PROJECT_KEY;

/**
 * A {@link org.sonar.api.notifications.NotificationDispatcher} that dispatches
 * <i>auto-assigned-new-issue</i> {@link Notification}s</i>
 *
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "AutoAssignedNewIssue";

  private final NotificationManager notificationManager;

  public static NotificationDispatcherMetadata newMetadata() {
    return NotificationDispatcherMetadata.create(KEY)
        .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
        .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
  }

  public AutoAssignedNewIssueNotificationDispatcher(NotificationManager notificationManager) {
    super(AutoAssignedNewIssueNotificationFactory.NOTIFICATION_TYPE_KEY);
    this.notificationManager = notificationManager;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String projectKey = notification.getFieldValue(PROJECT_KEY.name());
    Multimap<String, NotificationChannel> subscribedRecipients = notificationManager.findNotificationSubscribers(this, projectKey);
    addUserToContextIfSubscribed(context, notification.getFieldValue(ASSIGNEE.name()), subscribedRecipients);
  }

  private void addUserToContextIfSubscribed(Context context, @Nullable String user, Multimap<String, NotificationChannel> subscribedRecipients) {
    if (user != null) {
      Collection<NotificationChannel> channels = subscribedRecipients.get(user);
      for (NotificationChannel channel : channels) {
        context.addUser(user, channel);
      }
    }
  }


}
