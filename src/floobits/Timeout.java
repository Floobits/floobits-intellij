package floobits;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class Timeout extends RunLater implements Comparable {
    long timeout = Calendar.getInstance().getTimeInMillis();
    protected AtomicBoolean canceled = new AtomicBoolean(false);

    protected Timeout(long timeFromNow) {
        super(null);
        timeout += timeFromNow;
    }

    public void cancel() {
        this.canceled.set(true);
    }

    public Boolean isCanceled() {
        return this.canceled.get();
    }
    @Override
    public int compareTo(Object o) {
        return (int) (this.timeout - ((Timeout) o).timeout);
    }
}
