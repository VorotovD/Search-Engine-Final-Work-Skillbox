package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean();

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        indexingProcessing.set(false);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if(indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("'result' : false, " +
                    "'error' : Индексация уже запущена");
        } else {
            indexingProcessing.set(true);
            Runnable start = () -> indexingService.startIndexing(indexingProcessing);
            new Thread(start).start();
            return ResponseEntity.status(HttpStatus.OK).body("'result' : true");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        if (!indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("'result' : false, " +
                    "'error' : Индексация не запущена");
        } else {
            indexingProcessing.set(false);
            return ResponseEntity.status(HttpStatus.OK).body("'result' : true ");
        }
    }
}
