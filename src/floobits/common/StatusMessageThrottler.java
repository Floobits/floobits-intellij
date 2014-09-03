package floobits.common;

import floobits.common.interfaces.IContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * This class prevents status message spam by throttling messages.
 */
public class StatusMessageThrottler {
    private String throttleMessage;
    private int maxMessages = 10;
    private int throttleWait = 5;
    private List<String> messages = new ArrayList<String>();
    private ScheduledFuture schedule = null;
    private IContext context;

    /**
     * Create a message throttler that doesn't spam the user.
     * @param context
     * @param throttleMessage If this message has a %d it will show a message count.
     */
    public StatusMessageThrottler (IContext context, String throttleMessage) {
        this.throttleMessage = throttleMessage;
    }

    public void statusMessage(String message) {
        messages.add(message);
        queueUpMessages();
    }

    private void queueUpMessages() {
        if (schedule != null) {
            schedule.cancel(false);
        }
        schedule = context.setTimeout(throttleWait, new Runnable() {
            @Override
            public void run() {
                clearMessages();
            }
        });
    }

    private void clearMessages() {
        int numMessages = messages.size();
        if (numMessages > maxMessages) {
            if (throttleMessage.contains("%d")) {
                context.statusMessage(String.format(throttleMessage, numMessages));
            } else {
                context.statusMessage(throttleMessage);
            }
            for (String message : messages) {
                context.chatStatusMessage(message);
            }
            return;
        }
        for (String message : messages) {
            context.statusMessage(message);
        }
        messages.clear();
    }
}
