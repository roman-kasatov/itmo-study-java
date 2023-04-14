package info.kgeorgiy.ja.kasatov.concurrent;


import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

public class IterativeParallelism implements ListIP {

    /**
     * Split a {@code list}.
     * First {@code list.size() % threads} lists have length {@code x + 1} and the rest {@code x},
     * where {@code x} equals {@code list.size() / threads}.
     * @param threads number of parts to split into.
     * @return List with lists where last several (possibly all) can be empty.
     */
    private <T> List<List<? extends T>> splitToLists(int threads, List<? extends T> list) {
        List<List<? extends T>> result = new ArrayList<>(threads);
        int start, end = 0;
        for (int i = 0; i < threads; i++) {
            start = end;
            end = (i + 1) * (list.size() / threads) + Math.min(i + 1, list.size() % threads);
            result.add(list.subList(start, end));
        }
        return result;
    }

    /**
     * Apply a function to {@code list} by parts and then merge results.
     * Each part processed in a separate thread.
     * @param threads number of parts.
     * @param function function applied to each part.
     * @param mergeFunction function that combines results for parts.
     * @throws InterruptedException if executing thread was interrupted.
     */
    private <T, U, R> R applyInParallel(
            int threads,
            List<? extends T> list,
            Function<List<? extends T>, U> function,
            Function<List<U>, R> mergeFunction
    ) throws InterruptedException {
        List<List<? extends T>> splittedList = splitToLists(threads, list);
        List<U> results = new ArrayList<>(Collections.nCopies(threads, null));
        List<Thread> threadsList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int finalI = i;
            Thread thread = new Thread(() ->
                    results.set(finalI, function.apply(splittedList.get(finalI)))
            );
            threadsList.add(thread);
            thread.start();
        }
        try {
            for (Thread t : threadsList) {
                t.join();
            }
        } catch (InterruptedException e) {
            for (Thread t : threadsList) {
                t.interrupt();
            }
            // :NOTE: не дожидаешься завершения созданных потоков
            throw new InterruptedException("One of threads was interrupted " +
                    "while applying function in parallel: " + e);
        }
        return mergeFunction.apply(results);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().filter(Objects::nonNull).max(comparator).orElse(null),
                list -> list.stream().filter(Objects::nonNull).max(comparator).orElseThrow()
        );
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().allMatch(predicate),
                list -> list.stream().allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().filter(predicate).count(),
                list -> list.stream().reduce(0L, Long::sum).intValue()
        );
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().map(Object::toString).collect(Collectors.joining()),
                list -> String.join("", list)
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().filter(predicate).toList(),
                list -> list.stream().flatMap(List::stream)
                        .collect(Collectors.toCollection(ArrayList<T>::new))
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return applyInParallel(
                threads,
                values,
                list -> list.stream().map(f).toList(),
                list -> list.stream().flatMap(List::stream)
                        .collect(Collectors.toCollection(ArrayList<U>::new))
        );
    }
}
