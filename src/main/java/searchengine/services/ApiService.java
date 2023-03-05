package searchengine.services;

import java.util.concurrent.atomic.AtomicBoolean;

public interface ApiService {
    void startIndexing(AtomicBoolean indexingProcessing);
}
