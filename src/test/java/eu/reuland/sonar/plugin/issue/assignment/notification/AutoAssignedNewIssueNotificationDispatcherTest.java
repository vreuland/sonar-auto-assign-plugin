package eu.reuland.sonar.plugin.issue.assignment.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;

import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationFactory.NOTIFICATION_TYPE_KEY;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.ASSIGNEE;
import static eu.reuland.sonar.plugin.issue.assignment.notification.AutoAssignedNewIssueNotificationField.PROJECT_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Vincent Reuland
 */
public class AutoAssignedNewIssueNotificationDispatcherTest {

  private NotificationChannel emailChannel = mock(NotificationChannel.class);
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);

  private AutoAssignedNewIssueNotificationDispatcher dispatcher =
      new AutoAssignedNewIssueNotificationDispatcher(notificationManager);

  @Test
  public void shouldNotDispatchIfNotAutoAssignedNewIssueNotification() throws Exception {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);
    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void shouldDispatchToIssueAssignee() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("issueAssignee", emailChannel);
    recipients.put("otherUser", emailChannel);
    when(notificationManager.findNotificationSubscribers(dispatcher, "struts")).thenReturn(recipients);

    Notification notification = new Notification(NOTIFICATION_TYPE_KEY)
        .setFieldValue(PROJECT_KEY.name(), "struts")
        .setFieldValue(ASSIGNEE.name(), "issueAssignee");

    dispatcher.performDispatch(notification, context);

    verify(context).addUser("issueAssignee", emailChannel);
    verifyNoMoreInteractions(context);
  }
}
