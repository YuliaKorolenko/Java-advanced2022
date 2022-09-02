package info.kgeorgiy.ja.korolenko.concurrent;


import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threadsList;
    private final Queue<Runnable> queue;

    /**
     * Constructor for ParallelMapperImpl. Create threads with current tasks in queue.
     *
     * @param threads count of threads are allowed to use.
     */
    public ParallelMapperImpl(final int threads) {
        if (0 >= threads) {
            throw new IllegalArgumentException("Unable to work with none or negative count of threads");
        }
        queue = new ArrayDeque<>();
        threadsList = new ArrayList<>();
        final Runnable term = () -> {
            try {
                while (!Thread.interrupted()) {
                    final Runnable task;
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            queue.wait();
                        }
                        task = queue.remove();
                    }
                    task.run();
                }
            } catch (final InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        for (int i = 0; i < threads; i++) {
            final Thread thread = new Thread(term);
            thread.start();
            threadsList.add(thread);
        }
    }

    /**
     * @param f    function for operation on args
     * @param args values to operate on
     * @throws InterruptedException is thrown if thread is interrupted
     * @ result values is result of work function on args
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final List<R> answer = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Helper helper = new Helper(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;
            final T a = args.get(i);
            synchronized (queue) {
                queue.add(() -> {
                    try {
                        answer.set(finalI, f.apply(a));
                        helper.decreaseCounter();
                    } catch (final RuntimeException e) {
                        helper.setException(e);
                    }
                });
                queue.notify();
            }
        }
        helper.endHelper();
        return answer;
    }

    /**
     * method, which close threads
     */
    @Override
    public void close() {
        threadsList.forEach(Thread::interrupt);
        for (int i = 0; i < threadsList.size(); i++) {
            try {
                threadsList.get(i).join();
            } catch (final InterruptedException ignored) {
                i--;
            }
        }
    }

    /**
     * Helper class for control count of threads and storage exceptions
     */
    public static class Helper {
        private int counter;
        private RuntimeException exception;

        Helper(final int counter) {
            this.counter = counter;
            this.exception = null;
        }

        public synchronized void setException(final RuntimeException e) {
            if (this.exception == null) {
                this.exception = e;
            } else {
                this.exception.addSuppressed(e);
            }
        }

        public synchronized void endHelper() throws InterruptedException {
            while (this.counter > 0) {
                wait();
            }
            if (this.exception != null) {
                throw this.exception;
            }
        }

        public synchronized void decreaseCounter() {
            counter--;
            if (counter <= 0) {
                notify();
            }
        }

    }
}
