package floobits.common;

import floobits.utilities.Flog;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

public class Timeouts extends Thread {
        protected ConcurrentSkipListSet<Timeout> timeouts = new ConcurrentSkipListSet<Timeout>();
    private Timer autoUpdate;

    public static Timeouts create() {
        final Timeouts timeouts1 = new Timeouts();
        timeouts1.start();
        return timeouts1;
    }
    public Timeouts () {}

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

    @Override
    public void run() {
        super.run();
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
                       Flog.info("timeout ran %s", t);
                       t.runnable.run();
                   }
                   timeouts.remove(t);
               }
            }
        }, 0, 250);
    }
}
