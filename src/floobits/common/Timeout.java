package floobits.common;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class Timeout implements Comparable{
    long timeout = Calendar.getInstance().getTimeInMillis();
    public Runnable runnable;
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    public Timeout(long timeFromNow, Runnable runnable) {
        timeout += timeFromNow;
        this.runnable = runnable;
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
