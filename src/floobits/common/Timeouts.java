package floobits.common;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

public class Timeouts {
    protected ConcurrentSkipListSet<Timeout> timeouts = new ConcurrentSkipListSet<Timeout>();
    private Timer autoUpdate;

    public Timeouts () {
        autoUpdate = new Timer();
        autoUpdate.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
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
