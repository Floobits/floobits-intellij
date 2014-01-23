package floobits;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

public class Timeouts extends Thread {
        protected ConcurrentSkipListSet<Timeout> timeouts = new ConcurrentSkipListSet<Timeout>();

    public static Timeouts create() {
        final Timeouts timeouts1 = new Timeouts();
        timeouts1.start();
        return timeouts1;
    }
    public Timeouts () {}

    public void setTimeout(final Timeout timeout) {
        timeouts.add(timeout);
    }

    @Override
    public void run() {
        super.run();
        Timer autoUpdate = new Timer();
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
                       t.run(null);
                   }
                   timeouts.remove(t);
               }
            }
        }, 0, 250);
    }
}
