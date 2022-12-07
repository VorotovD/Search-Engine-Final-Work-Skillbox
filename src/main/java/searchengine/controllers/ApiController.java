package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private boolean indexingProcessing = false;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if(indexingProcessing) {
            return ResponseEntity.status(HttpStatus.PROCESSING).body("'result' : false\n " +
                    "'error' : Индексация уже запущена");
        } else {
            indexingProcessing = true;
            StatisticsResponse response = statisticsService.getStatistics();
            indexingProcessing = false;
            return ResponseEntity.status(HttpStatus.OK).body(response + "\n'result' : true");
        }
    }
}
