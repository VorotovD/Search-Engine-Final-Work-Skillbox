package searchengine.services.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.Page;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class PageIndexer extends RecursiveTask<Map<String,Page>> {
    private final Connection connection;
    private final String domain;
    private final Set<String> urlSet = new HashSet<>();
    private final String site;
    private final Map<String,Page> resultForkJoinPoolIndexedPages;

    public PageIndexer(String domain, String site, Map<String,Page> resultForkJoinPoolIndexedPages,Connection connection) {
        this.domain = domain;
        this.site = site;
        this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;

        this.connection = connection;
    }


    @Override
    protected Map<String,Page> compute() {
        Page indexingPage = new Page();
        indexingPage.setPath(site);
        try {
            Document doc = Jsoup.connect(domain).
                    userAgent(connection.getUserAgent()).
                    referrer(connection.getReferer()).
                    get();
            //Document doc = Jsoup.connect(domain).get();
            indexingPage.setContent(String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");
            //TODO нужны только ссылки доменного сайта, отсечь Домен для внесения в urlSet
            pages.forEach(page -> {
                if (page.attr("href").contains(domain.replace("www",""))) {
                    String [] urlToSet = page.attr("href").split(domain);
                    urlSet.add(urlToSet[0]);
                }
                if (page.attr("href").charAt(0) == '/') {
                    urlSet.add(page.attr("href"));
                }
            });
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (IOException ex) {
            indexingPage.setCode(500);
            System.out.println("Catch exception");
        }
        resultForkJoinPoolIndexedPages.put(indexingPage.getPath(),indexingPage);

        List<PageIndexer> indexingPagesTasks = new ArrayList<>();
        for (String url: urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null) {
                PageIndexer task = new PageIndexer(domain, url, resultForkJoinPoolIndexedPages,connection);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (PageIndexer page: indexingPagesTasks) {
            page.join();
        }


        return resultForkJoinPoolIndexedPages;
    }
}
