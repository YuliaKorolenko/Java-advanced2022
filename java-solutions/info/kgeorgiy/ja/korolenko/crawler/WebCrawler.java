package info.kgeorgiy.ja.korolenko.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.downloader = downloader;
        this.perHost = perHost;
    }

    private void bfs(BlockingQueue<String> currentUrl, BlockingQueue<String> nextUrl, Phaser phaser, boolean marker, final Set<String> visited, Map<String, IOException> errors) {
        for (String url : currentUrl) {
            phaser.register();
            downloaders.execute(() -> {
                try {
                    Document document = downloader.download(url);
                    if (!marker) {
                        phaser.register();
                        extractors.execute(() -> {
                            try {
                                for (String links : document.extractLinks()) {
                                    if (visited.add(links)) {
                                        nextUrl.add(links);
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        });
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
    }

    @Override
    public Result download(String url, int depth) {
        BlockingQueue<String> currentUrl = new LinkedBlockingDeque<>();
        final Phaser ph = new Phaser(1);
        final Set<String> visited = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        currentUrl.add(url);
        visited.add(url);
        for (int i = 0; i < depth; i++) {
            BlockingQueue<String> nextUrl = new LinkedBlockingDeque<>();
            bfs(currentUrl, nextUrl, ph, i + 1 == depth, visited, errors);
            ph.arriveAndAwaitAdvance();
            currentUrl = nextUrl;
        }
        visited.removeAll(errors.keySet());
        return new Result(visited.stream().toList(), errors);
    }

    @Override
    public void close() {
        boolean errorExtractors = shutDownAndAwaitTerminator(extractors);
        boolean errorDownloaders = shutDownAndAwaitTerminator(downloaders);
        if (errorDownloaders || errorExtractors) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shutDownAndAwaitTerminator(ExecutorService executorService) {
        boolean isError = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            isError = true;
        }
        return isError;
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 5) {
            System.out.println("Wrong arguments");
        }
        try {
            int length = args.length;
            int downloaders = length > 1 ? Integer.parseInt(args[1]) : 4;
            int extractors = length > 2 ? Integer.parseInt(args[2]) : 4;
            int perHost = length > 3 ? Integer.parseInt(args[3]) : 4;
            int depth = length > 4 ? Integer.parseInt(args[4]) : 1;
            try {
                Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost);
                System.out.println(crawler.download(args[0], depth).getDownloaded());
                crawler.close();
            } catch (IOException e) {
                System.out.println("Can't create CashingDownloader : " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Wrong format of argument :" + e.getMessage());
        }
    }
}
