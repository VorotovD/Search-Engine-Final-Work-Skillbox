package searchengine.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SearchDataResponse {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double relevance;
}
