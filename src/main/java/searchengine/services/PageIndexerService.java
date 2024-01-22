package searchengine.services;

import searchengine.model.Page;

import java.io.IOException;
import java.util.Map;

public interface PageIndexerService {
    void indexHtml(String html, Page indexingPage);

    void refreshIndex(String html, Page refreshPage);
}
