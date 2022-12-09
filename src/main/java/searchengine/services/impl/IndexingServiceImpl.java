package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SitePage> sitePages;
    private final Connection connection;
    @Override
    public ResponseEntity startIndexing() {
        ResponseEntity responseResult = ResponseEntity.status(HttpStatus.OK).body("\n'result' : true");
        try {
            deleteSitesAndPagesInDB();
            addSitePagesToBD();
            indexAllSitePages();
            //indexSitePage(siteApp.getUrl());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex);
        }


        return responseResult;
    }

    private void deleteSitesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl())){
                    siteRepository.deleteById(sitePageDb.getId());
                }
            }
        }
    }

    private void addSitePagesToBD() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePageDAO = new SitePage();
            sitePageDAO.setStatus(Status.INDEXING);
            sitePageDAO.setName(siteApp.getName());
            sitePageDAO.setUrl(siteApp.getUrl());
            siteRepository.save(sitePageDAO);
        }

    }

    private void indexAllSitePages() {
        //TODO в sitepages может быть больше сайтов чем в application
        //TODO запулить индексацию в отдельный поток
        sitePages.addAll(siteRepository.findAll());
        for (SitePage site :sitePages) {
            Map<String,Page> indexedPages = indexSitePage(site.getUrl());
            addPagesToBD(site.getId(),indexedPages);
        }
    }
    private void addPagesToBD(int siteId, Map<String,Page> indexedPages) {
        for (Page page: indexedPages.values()) {
            page.setSiteId(siteId);

        }
    }
    private Map<String,Page> indexSitePage(String domain) {
        Map<String,Page> resultSet = new HashMap<>();
        return new ForkJoinPool().invoke(new PageIndexer(domain, "",resultSet,connection));
    }
}
