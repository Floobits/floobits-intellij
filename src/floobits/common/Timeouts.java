package floobits.common;

import floobits.BaseContext;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

public class Timeouts {
    protected ConcurrentSkipListSet<Timeout> timeouts = new ConcurrentSkipListSet<Timeout>();
    private Timer autoUpdate;

    public Timeouts(final BaseContext context) {
        autoUpdate = new Timer();
        autoUpdate.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    long now = Calendar.getInstance().getTimeInMillis();

                    for (Timeout t : timeouts) {
                        if (t.timeout > now) {
                            return;
                        }
                        if (!t.isCanceled()) {
                            t.runnable.run();
                        }
                        timeouts.remove(t);
                    }
                } catch (Throwable e) {
                    API.uploadCrash(context, e);
                }
            }
        }, 0, 250);
    }

    public void setTimeout(final Timeout timeout) {
        timeouts.add(timeout);
    }

    public void shutdown() {
        for (Timeout timeout : timeouts) {
          timeout.cancel();
        }
        timeouts.clear();
        if (autoUpdate != null){
            autoUpdate.cancel();
            autoUpdate = null;
        }
    }
}
