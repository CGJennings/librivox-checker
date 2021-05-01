package ca.cgjennings.apps.librivox;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * A singleton that manages the analysis of audio files in parallel using a pool
 * of threads. The manager limits the number of jobs that are allowed to run in
 * parallel based on the number of CPUs, thus preventing the system from being
 * overwhelmed when a large number of files are added to the list of files to
 * check.
 *
 * <p>
 * <b>Note:</b> This package-private class is used internally
 * {@link LibriVoxAudioFile}. It is intended only for use by that class.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.3
 */
class JobManager {

    /**
     * This class cannot be instantiated.
     */
    private JobManager() {
    }

    /**
     * Set this to a positive number to use exactly that many threads in the
     * thread pool (for debugging purposes). If this is less than 1, then the
     * number of threads will be the total number of CPUs/cores plus one. (The
     * analysis process switches between file I/O and computation as it reads
     * the file, so two files can reasonably be worked on in parallel even if
     * there is only 1 CPU.)
     */
    private static final int DEBUG_THREAD_COUNT = -1;

    /**
     * If <code>true</code>, then the threads created for the thread pool will
     * request higher than normal priority. The mapping of the this request to a
     * native thread priority on the host system is platform-dependent, so it
     * may or may not have an effect on performance.
     */
    private static boolean USE_HIGHER_PRIORITY_THREADS = false;

    /**
     * The length of time, in ms, to keep a thread around waiting for a new job
     * before allowing it to die.
     */
    private static final long KEEP_ALIVE_TIME = 1000L * 60L * 15L;

    /**
     * This factory is used to create new threads for the pool on demand.
     */
    private static final ThreadFactory threadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "checker worker thread #" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            if (USE_HIGHER_PRIORITY_THREADS) {
                t.setPriority(Thread.NORM_PRIORITY + (Thread.MAX_PRIORITY - Thread.NORM_PRIORITY) / 2);
            }
            return t;
        }
        private final AtomicInteger threadCounter = new AtomicInteger(0);
    };

    /**
     * The actual pool of threads used by the manager.
     */
    private static final ThreadPoolExecutor threadPool;

    static {
        final int cpus = DEBUG_THREAD_COUNT < 1
                ? Math.max(2, Runtime.getRuntime().availableProcessors())
                : DEBUG_THREAD_COUNT;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        threadPool = new ThreadPoolExecutor(cpus, cpus, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, workQueue, threadFactory);
        threadPool.prestartAllCoreThreads();
    }

    /**
     * Adds an audio file to the queue of files to be analyzed
     *
     * @param file the file to add to the queue
     * @return a {@link JobToken} that can be used to control the job
     */
    public static JobToken analyzeInFuture(LibriVoxAudioFile file, Runnable job) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        Future<?> f = threadPool.submit(job);
        return new JobToken(file, job, f);
    }

    /**
     * A <code>JobToken</code> is returned when a file is submitted to the
     * manager by calling {@link #analyzeInFuture}. The token can be used to
     * cancel a queued job before it starts running.
     */
    public static class JobToken {

        private LibriVoxAudioFile file;
        private Runnable r;
        private Future<?> f;

        private JobToken(LibriVoxAudioFile file, Runnable r, Future<?> f) {
            this.file = file;
            this.r = r;
            this.f = f;
        }

        /**
         * Returns the file that this token controls.
         *
         * @return the file that, when submitted to the job manager, produced
         * this token
         */
        public LibriVoxAudioFile getFile() {
            return file;
        }

        /**
         * Cancels this job. If the job hasn't started, it will never be run. If
         * it has started but hasn't finished, the thread's interrupt status
         * will be set. If the job has already finished, this has no effect.
         */
        public void cancel() {
            f.cancel(true);
        }

        /**
         * Waits until the analysis job has completed, normally or not.
         */
        public void waitUntilDone() {
            boolean done = false;
            do {
                done = true;
                try {
                    if (!f.isCancelled()) {
                        f.get();
                    }
                } catch (InterruptedException e) {
                    done = false;
                } catch (ExecutionException e) {
                    Checker.getLogger().log(Level.SEVERE, "uncaught exception in analysis job", e);
                }
            } while (!done);
        }

        /**
         * Waits up to ms milliseconds for the job to finish.
         *
         * @param ms the maximum wait time
         * @throws InterruptedException if the waiting thread is interrupted
         */
        public void waitFor(int ms) throws InterruptedException {
            try {
                if (!f.isCancelled()) {
                    f.get(ms, TimeUnit.MILLISECONDS);
                }
            } catch (ExecutionException e) {
                Checker.getLogger().log(Level.SEVERE, "uncaught exception in analysis job", e);
            } catch (CancellationException e) {
                // do nothing
            } catch (TimeoutException e) {
                // do nothing
            }
        }

        /**
         * Returns <code>true</code> if the job has been cancelled.
         *
         * @return <code>true</code> if {@link #cancel} was called successfully
         */
        public boolean isCancelled() {
            return f.isCancelled();
        }

        /**
         * Returns <code>true</code> if the job has started and then either
         * cancelled or finished.
         *
         * @return
         */
        public boolean isDone() {
            return f.isDone();
        }

        @Override
        public String toString() {
            return "[Job " + file.getFileName() + ": cancelled=" + f.isCancelled() + ", done=" + f.isDone() + "]";
        }
    }
}
