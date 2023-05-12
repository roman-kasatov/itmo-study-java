package info.kgeorgiy.ja.kasatov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final int perHost;
    private final Downloader downloader;
    private final ExecutorService extractors;
    private final ExecutorService downloaders;
    private final Map<String, Host> hosts = new ConcurrentHashMap<>();


    private enum DEFAULT_ARGUMENTS {
        DEPTH(1),
        DOWNLOADS(10),
        EXTRACTORS(10),
        PER_HOST(10);
        public final int value;
        DEFAULT_ARGUMENTS(int value) {
            this.value = value;
        }
    }


    private class Host {
        private int load = 0;
        private final Queue<Runnable> taskQueue = new ConcurrentLinkedDeque<>();

        public synchronized void addTask(Runnable task) {
            if (load < perHost) {
                downloaders.submit(task);
                load++;
            } else {
                taskQueue.add(task);
            }
        }
        public synchronized void endTask() {
            load--;
            if (load < perHost && !taskQueue.isEmpty()) {
                downloaders.submit(taskQueue.poll());
                load++;
            }
        }
    }

    private class Worker {

        private final ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();
        private final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        private final Set<String> visited = ConcurrentHashMap.newKeySet();
        private final Phaser phaser = new Phaser(1);

        private void addExtractTask(
                Document document,
                String url,
                Queue<String> queue
        ) {
            phaser.register();
            extractors.submit(() -> {
                try {
                    List<String> nextUrls = document.extractLinks();
                    for (final String nextUrl : nextUrls) {
                        if (visited.add(nextUrl)) {
                            queue.add(nextUrl);
                        }
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
        private void addDownloadTask(String url, Queue<String> nextDepthQueue, boolean goDeeper) {
            try {
                Host host = hosts.computeIfAbsent(URLUtils.getHost(url), s -> new Host());
                phaser.register();
                host.addTask(() -> {
                    try {
                        Document document = downloader.download(url);
                        downloaded.add(url);
                        if (goDeeper) {
                            addExtractTask(document, url, nextDepthQueue);
                        }
                    } catch (IOException e) {
                        errors.put(url, e);
                    } finally {
                        host.endTask();
                        phaser.arriveAndDeregister();
                    }
                });
            } catch (MalformedURLException e) {
                errors.put(url, e);
            }
        }

        private Result download(
                String url,
                int depth
        ) {

            final Queue<String> currentDepthQueue = new ConcurrentLinkedQueue<>();
            final Queue<String> nextDepthQueue = new ConcurrentLinkedQueue<>();
            currentDepthQueue.add(url);
            visited.add(url);

            for (int curDepth = depth; curDepth > 0; curDepth--){
                for (final String currentUrl : currentDepthQueue) {
                    addDownloadTask(currentUrl, nextDepthQueue, curDepth > 1);
                }
                phaser.arriveAndAwaitAdvance();
                currentDepthQueue.clear();
                currentDepthQueue.addAll(nextDepthQueue);
                nextDepthQueue.clear();
            }

            return new Result(new ArrayList<>(downloaded), errors);
        }
    }

    private static int parseArgument(String[] args, int nmb, DEFAULT_ARGUMENTS defaultEnumElement) {
        return args.length > nmb
                ? Integer.parseInt(args[nmb])
                : defaultEnumElement.value;
    }

    /**
     * Creates new WebCrawler and downloads by url with arguments.
     * @param args arguments for {@link WebCrawler#WebCrawler(Downloader, int, int, int)}
     *             and {@link WebCrawler#download(String, int)} in following format
     *             [depth [downloads [extractors [perHost]]]] where default values are: <br>
     *             depth: {@link DEFAULT_ARGUMENTS#DEPTH} <br>
     *             downloads: {@link DEFAULT_ARGUMENTS#DOWNLOADS} <br>
     *             extractors: {@link DEFAULT_ARGUMENTS#EXTRACTORS} <br>
     *             perHost: {@link DEFAULT_ARGUMENTS#PER_HOST}
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("URL expected");
        }
        try (WebCrawler crawler = new WebCrawler(
                new CachingDownloader(0),
                parseArgument(args, 2, DEFAULT_ARGUMENTS.DOWNLOADS),
                parseArgument(args, 3, DEFAULT_ARGUMENTS.EXTRACTORS),
                parseArgument(args, 4, DEFAULT_ARGUMENTS.PER_HOST)
        )) {
            crawler.download(
                    args[0],
                    parseArgument(args, 1, DEFAULT_ARGUMENTS.DEPTH)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    @Override
    public Result download(String url, int depth) {
        return new Worker().download(url, depth);
    }

    @Override
    public void close() {
        downloaders.shutdownNow();
        extractors.shutdownNow();
        try {
            if ( !downloaders.awaitTermination(1000, TimeUnit.MILLISECONDS) ||
                    !extractors.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                System.err.println("ExecutorService's thread pool wasn't terminated");
            }
        } catch (InterruptedException e) {
            System.err.println("Was interrupted while waiting for " +
                    "ExecutorService to terminate");
        }
    }
}