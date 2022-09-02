package info.kgeorgiy.ja.korolenko.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterativeParallelism implements ScalarIP {

    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("List is empty");
        }
        return createThreads(threads, values, stream -> stream.max(comparator).orElseThrow(), stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("List is empty");
        }
        return createThreads(threads, values, stream -> stream.min(comparator).orElseThrow(), stream -> stream.min(comparator).orElseThrow());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return createThreads(threads, values, stream -> stream.anyMatch(predicate), stream -> stream.anyMatch(Boolean::booleanValue));
    }

    /**
     * @param threads        number or concurrent threads.
     * @param values         values to test.
     * @param function       for each thread.
     * @param resultFunction for join threads.
     * @param <T>            value type.
     * @param <R>            result type
     * @return R resul type of function
     * @throws InterruptedException if executing thread was interrupted.
     */
    private <T, R> R createThreads(int threads, final List<T> values, final Function<Stream<T>, R> function,
                                   final Function<Stream<R>, R> resultFunction
    ) throws InterruptedException {
        final List<Thread> threadsList = new ArrayList<>();
        threads = Math.min(threads, values.size());

        final int interval = values.size() / threads;
        final int remTask = values.size() % threads;

        final List<R> answerThreads;
        final List<Stream<T>> streamsList = new ArrayList<>();
        int previous = 0;
        for (int i = 0; i < threads; i++) {
            final int finalBegin = previous;
            final int finalEnd = finalBegin + interval + (i < remTask ? 1 : 0);
            streamsList.add(i, values.subList(finalBegin, finalEnd).stream());
            previous = finalEnd;
        }

        if (parallelMapper != null) {
            answerThreads = parallelMapper.map(function, streamsList);
        } else {
            answerThreads = new ArrayList<>(Collections.nCopies(threads,null));
            for (int i = 0; i < threads; i++) {
                final int finalI = i;
                final Stream<T> s = streamsList.get(i);
                final Thread thread = new Thread(() ->
                        answerThreads.set(finalI, function.apply(s)));
                thread.start();
                threadsList.add(thread);
            }
            for (final Thread thread : threadsList) {
                thread.join();
            }
        }
        return resultFunction.apply(answerThreads.stream());
    }
}
