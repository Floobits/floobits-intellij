package floobits.common;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class Timeout extends RunLater<Void> implements Comparable {
    long timeout = Calendar.getInstance().getTimeInMillis();
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    protected Timeout(long timeFromNow) {
        super();
        timeout += timeFromNow;
    }

    public void cancel() {
        this.canceled.set(true);
    }

    public Boolean isCanceled() {
        return this.canceled.get();
    }
    @Override
    public int compareTo(@NotNull Object o) {
        return (int) (this.timeout - ((Timeout) o).timeout);
    }
}
