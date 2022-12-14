package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SitePage> sitePagesAllFromDB;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    @Override
    public ResponseEntity startIndexing() {
        ResponseEntity responseResult = ResponseEntity.status(HttpStatus.OK).body("\n'result' : true");
        try {
            deleteSitesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
            //indexSitePage(siteApp.getUrl());
        } catch (RuntimeException ex) {
            logger.error("Error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex);
        }


        return responseResult;
    }

    private void deleteSitesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl())) {
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
            sitePageDAO.setUrl(siteApp.getUrl());
            siteRepository.save(sitePageDAO);
        }

    }

    private void indexAllSitePages() {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));


        for (SitePage site : sitePagesAllFromDB) {
            Map<String, Page> indexedPages = indexSitePage(site.getUrl());
            System.out.println("Проиндексирован сайт: " + site.getName());
            SitePage sitePage = siteRepository.findById(site.getId()).orElseThrow();
            addPagesToDB(sitePage, indexedPages);
            sitePage = siteRepository.findById(site.getId()).orElseThrow();
            sitePage.setStatus(Status.INDEXED);
            siteRepository.save(sitePage);
        }
    }

    private void addPagesToDB(SitePage sitePage, Map<String, Page> indexedPages) {
        for (Page page : indexedPages.values()) {
            page.setSiteId(sitePage.getId());
            pageRepository.save(page);

            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(sitePage);
        }
    }

    private Map<String, Page> indexSitePage(String domain) {
        Map<String, Page> resultSet = new HashMap<>();
        return new ForkJoinPool().invoke(new PageIndexer(domain, "/", resultSet, connection));
    }
}
