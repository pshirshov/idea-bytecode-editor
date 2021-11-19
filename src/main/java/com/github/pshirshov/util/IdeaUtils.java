package com.github.pshirshov.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;

public final class IdeaUtils {
    private IdeaUtils() {
    }

    public static void showErrorNotification(String title, Throwable t) {
        Notification notification = new Notification("scalagen", title, ExceptionUtils
                .toString(t), NotificationType.ERROR, null);
        ApplicationManager
                .getApplication()
                .getMessageBus()
                .syncPublisher(Notifications.TOPIC)
                .notify(notification);
    }

    public static int findSubstringOffset(String code, Document document, int lineNumber, String marker) {
        int offset;
        offset = code.indexOf(marker + lineNumber);
        while (offset == -1 && lineNumber < document.getLineCount()) {
            offset = code.indexOf(marker + (lineNumber++));
        }
        return offset;
    }
}
