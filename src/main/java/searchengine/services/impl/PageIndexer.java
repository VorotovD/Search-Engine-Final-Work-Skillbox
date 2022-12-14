package searchengine.services.impl;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.Page;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class PageIndexer extends RecursiveTask<Map<String, Page>> {
    private final Connection connection;
    private final String domain;
    private final Set<String> urlSet = new HashSet<>();
    private final String site;
    private final Map<String, Page> resultForkJoinPoolIndexedPages;

    public PageIndexer(String domain, String site, Map<String, Page> resultForkJoinPoolIndexedPages, Connection connection) {
        this.domain = domain;
        this.site = site;
        this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;
        this.connection = connection;
    }

    @SneakyThrows
    @Override
    protected Map<String, Page> compute() {
        //Если блокирует доступ к страницам - использовать ->
        //Thread.sleep(1000);
        Page indexingPage = new Page();
        indexingPage.setPath(site);
        if (resultForkJoinPoolIndexedPages.get(site) != null) {
            return resultForkJoinPoolIndexedPages;
        }
        try {
            org.jsoup.Connection connect = Jsoup.connect(domain).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(10000).get();
            indexingPage.setContent(String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");
            pages.forEach(page -> {
                if (!page.attr("href").isEmpty() && page.attr("href").charAt(0) == '/') {
                    if (resultForkJoinPoolIndexedPages.get(page.attr("href")) == null) {
                        urlSet.add(page.attr("href"));
                    }
                }
            });
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (IOException ex) {
            indexingPage.setCode(522);
            System.out.println("Catch exception");
        }

        resultForkJoinPoolIndexedPages.putIfAbsent(indexingPage.getPath(), indexingPage);


        List<PageIndexer> indexingPagesTasks = new ArrayList<>();
        for (String url : urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null) {
                PageIndexer task = new PageIndexer(domain, url, resultForkJoinPoolIndexedPages, connection);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (PageIndexer page : indexingPagesTasks) {
            page.join();
        }

        return resultForkJoinPoolIndexedPages;
    }
}

