package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;

import java.net.MalformedURLException;

public interface StatisticsService {
    StatisticsResponse getStatistics() throws MalformedURLException;
}
