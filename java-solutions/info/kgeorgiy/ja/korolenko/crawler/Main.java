package info.kgeorgiy.ja.korolenko.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Crawler crawler = new WebCrawler(new CachingDownloader(), Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            System.out.println(crawler.download("", 2).getDownloaded());
            crawler.close();
        } catch (IOException e) {
            System.out.println("Can't create CashingDownloader");
        }
    }
}
