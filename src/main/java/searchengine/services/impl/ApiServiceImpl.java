package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceImpl implements ApiService {
    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SitePage> sitePagesAllFromDB;
    private final Connection connection;
    private AtomicBoolean indexingProcessing;


    @Override
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            indexingProcessing.set(false);
            log.error("Error: ", ex);
        }
    }

    @Override
    public void refreshPage(SitePage siteDomain, URL url) {
        SitePage existSitePate = siteRepository.getSitePageByUrl(siteDomain.getUrl());
        siteDomain.setId(existSitePate.getId());
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
            PageFinder f = new PageFinder(siteRepository, pageRepository, siteDomain, url.getPath(), resultForkJoinPageIndexer, connection, lemmaService, pageIndexerService, indexingProcessing);
            f.refreshPage();
        } catch (SecurityException ex) {
            SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }
        log.info("Проиндексирован сайт: " + siteDomain.getName());
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);
    }

    private void deleteSitePagesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl().toString())) {
                    siteRepository.deleteById(sitePageDb.getId());
                }
            }
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePageDAO = new SitePage();
            sitePageDAO.setStatus(Status.INDEXING);
            sitePageDAO.setName(siteApp.getName());
            sitePageDAO.setUrl(siteApp.getUrl().toString());
            siteRepository.save(sitePageDAO);
        }
    }

    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SitePage siteDomain : sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    log.info("Запущена индексация " + siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(siteRepository, pageRepository, siteDomain, "", resultForkJoinPageIndexer, connection, lemmaService, pageIndexerService, indexingProcessing));
                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }
                if (!indexingProcessing.get()) {
                    log.warn("Indexing stopped by user, site:" + siteDomain.getUrl());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    log.info("Проиндексирован сайт: " + siteDomain.getUrl());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }
}
