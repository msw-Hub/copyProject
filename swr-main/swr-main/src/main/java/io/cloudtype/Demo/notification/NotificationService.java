package io.cloudtype.Demo.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public NotificationService(NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    public void notifyUser(String userId, String message) {
        //userId에는 userEntity의 username에 해당하는 부분이 들어가야함.
        log.info("Sending notification to user: " + userId + ", message: " + message);
        notificationWebSocketHandler.sendNotificationToUser(userId, message);
    }

    public boolean isUserConnected(String userId) {
        return notificationWebSocketHandler.isUserConnected(userId);
    }
}
