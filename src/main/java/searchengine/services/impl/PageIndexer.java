package searchengine.services.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageIndexer extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;
    private final Connection connection;
    private final Set<String> urlSet = new HashSet<>();
    private final String page;
    private final SitePage siteDomain;
    private final ConcurrentHashMap<String, Page> resultForkJoinPoolIndexedPages;

    public PageIndexer(SiteRepository siteRepository, PageRepository pageRepository, SitePage siteDomain, String page, ConcurrentHashMap<String, Page> resultForkJoinPoolIndexedPages, Connection connection, AtomicBoolean indexingProcessing) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.page = page;
        this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;
        this.connection = connection;
        this.indexingProcessing = indexingProcessing;
        this.siteDomain = siteDomain;
    }

    @Override
    protected void compute() {

        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        Page indexingPage = new Page();
        indexingPage.setPath(page);
        indexingPage.setSiteId(siteDomain.getId());
        //Если блочат подключение, используй ->
        //Thread.sleep(1000);
        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl()+page).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(60000).get();

            indexingPage.setContent(doc.head() + String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");
            for (org.jsoup.nodes.Element element : pages)
                if (!element.attr("href").isEmpty() && element.attr("href").charAt(0) == '/') {
                    if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
                        return;
                    } else if (resultForkJoinPoolIndexedPages.get(element.attr("href")) == null) {
                        urlSet.add(element.attr("href"));
                    }
                }
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (Exception ex) {
            String message = ex.toString();
            int errorCode;
            if (message.contains("UnsupportedMimeTypeException")) {
                errorCode = 415;    // Ссылка на pdf, jpg, png документы
            } else if (message.contains("Status=401")) {
                errorCode = 401;    // На несуществующий домен
            } else if (message.contains("UnknownHostException")) {
                errorCode = 401;
            } else if (message.contains("Status=403")) {
                errorCode = 403;    // Нет доступа, 403 Forbidden
            } else if (message.contains("Status=404")) {
                errorCode = 404;    // // Ссылка на pdf-документ, несущ. страница, проигрыватель
            } else if (message.contains("Status=500")) {
                errorCode = 401;    // Страница авторизации
            } else if (message.contains("ConnectException: Connection refused")) {
                errorCode = 500;    // ERR_CONNECTION_REFUSED, не удаётся открыть страницу
            } else if (message.contains("SSLHandshakeException")) {
                errorCode = 525;
            } else if (message.contains("Status=503")) {
                errorCode = 503; // Сервер временно не имеет возможности обрабатывать запросы по техническим причинам (обслуживание, перегрузка и прочее).
            }else {
                errorCode = -1;
            }
            indexingPage.setCode(errorCode);
        }
        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        resultForkJoinPoolIndexedPages.putIfAbsent(indexingPage.getPath(), indexingPage);
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(sitePage);
        pageRepository.save(indexingPage);

        List<PageIndexer> indexingPagesTasks = new ArrayList<>();
        for (String url : urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null && indexingProcessing.get()) {
                PageIndexer task = new PageIndexer(siteRepository,pageRepository,sitePage,url,resultForkJoinPoolIndexedPages,connection,indexingProcessing);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (PageIndexer page : indexingPagesTasks) {
            if (!indexingProcessing.get()) {
                return ;
            }
            page.join();
        }

    }

}
