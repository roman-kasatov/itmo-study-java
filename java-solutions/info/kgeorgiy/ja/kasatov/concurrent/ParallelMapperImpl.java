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
    private final TaskQueue tasks = new TaskQueue();

    private static class TaskQueue {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        public synchronized void add(Runnable runnable) {
            queue.add(runnable);
            notify();
        }

        public synchronized Runnable get() throws InterruptedException{
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }
    }

    private static class WaitingList<T> {
        private int counter = 0;
        private final List<T> list;

        public WaitingList(int size) {
            this.list = new ArrayList<>(Collections.nCopies(size, null));
        }

        public synchronized void set(int index, T element) {
            list.set(index, element);
            if (++counter >= list.size()) {
                notify();
            }
        }

        public synchronized List<T> waitForFilling() throws InterruptedException{
            while (counter < list.size()) {
                wait();
            }
            return list;
        }
    }

    public ParallelMapperImpl(int threads) {
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(new Worker(tasks));
            this.threads.add(thread);
            thread.start();
        }
    }

    private record Worker(TaskQueue tasks) implements Runnable {

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    tasks.get().run(); // throws InterruptedException
                }
            } catch (InterruptedException e) {
                // Worker was interrupted
            }
        }
    }


    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        WaitingList<R> result = new WaitingList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            tasks.add(() -> result.set(finalI, f.apply(args.get(finalI))));
        }
        return result.waitForFilling();
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
