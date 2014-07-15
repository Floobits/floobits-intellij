package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.common.protocol.buf.Buf;
import floobits.utilities.Flog;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EditorScheduler {
    public final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
    private final IContext context;
    // buffer ids are not removed from readOnlyBufferIds
    private final Runnable dequeueRunnable = new Runnable() {
        @Override
        public void run() {
            if (queue.size() > 5) {
                Flog.log("Doing %s work", queue.size());
            }
            while (true) {
                // TODO: set a limit here and continue later
                Runnable action = queue.poll();
                if (action == null) {
                    return;
                }
                action.run();
            }
        }
    };

    class QueuedAction implements Runnable {
        public final Buf buf;
        public RunLater<Buf> runnable;

        QueuedAction(Buf buf, RunLater<Buf> runnable) {
            this.runnable = runnable;
            this.buf = buf;
        }
        public void run() {
            long l = System.currentTimeMillis();
            synchronized (buf) {
                runnable.run(buf);
            }
            long l1 = System.currentTimeMillis() - l;
            if (l1 > 200) {
                Flog.log("Spent %s in ui thread", l1);
            }
        }
    }

    public EditorScheduler(IContext context) {
        this.context = context;
    }

    public void shutdown() {
        reset();
    }

    public void queue(Buf buf, RunLater<Buf> runnable) {
        if (buf == null) {
            Flog.log("Buf is null abandoning adding new queue action.");
            return;
        }
        QueuedAction queuedAction = new QueuedAction(buf, runnable);
        queue(queuedAction);
    }

    public void queue(Runnable runnable) {
        queue.add(runnable);
        if (queue.size() > 1) {
            return;
        }
        context.writeThread(dequeueRunnable);
    }

    public void reset() {
        queue.clear();
    }

}
