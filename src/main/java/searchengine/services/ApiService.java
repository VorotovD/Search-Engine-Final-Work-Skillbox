package searchengine.services;

import searchengine.model.SitePage;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ApiService {
    void startIndexing(AtomicBoolean indexingProcessing);

    void refreshPage(SitePage sitePage, URL url);
}
