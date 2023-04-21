package info.kgeorgiy.ja.kasatov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads = new ArrayList<>();
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    private static class ListWithCounter<T> extends ArrayList<T> {
        private int counter = 0;

        public synchronized int getCounter() {
            return this.counter;
        }

        @Override
        public synchronized T set(int index, T element) {
            // :NOTE: тред просыпается много лишних раз
            this.notifyAll();
            this.counter++;
            return super.set(index, element);
        }
    }

    public ParallelMapperImpl(int threads) {
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(new Worker(tasks));
            this.threads.add(thread);
            thread.start();
        }
    }

    private record Worker(Queue<Runnable> tasks) implements Runnable {

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Runnable task;
                    // логику по работе с очередью можно объединить в один класс
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                        // :NOTE: нотифай здесь не нужен
                        tasks.notifyAll();
                    }
                    task.run();
                }
            } catch (InterruptedException e) {
                // ???
            }
        }
    }


    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ListWithCounter<R> result = new ListWithCounter<>();
        // :NOTE: Collections.nCopies
        for (int i = 0; i < args.size(); i++) {
            result.add(null);
        }
        synchronized (tasks) {
            for (int i = 0; i < args.size(); i++) {
                int finalI = i;
                tasks.add(() -> result.set(finalI, f.apply(args.get(finalI))));
            }
            tasks.notifyAll();
        }
        // :NOTE: лишняя синхронизация? можно вынести в сам каунтер
        synchronized (result) {
            while (result.getCounter() < args.size()) {
                result.wait();
            }
        }
        return result;
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        InterruptedException exception = null;
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                if (Objects.nonNull(exception)) {
                    exception.addSuppressed(e);
                } else {
                    exception = e;
                }
            }
        }
        if (Objects.nonNull(exception)) {
            System.out.println("Parallel mapping was interrupted: " + exception.getMessage());
        }
    }
}
