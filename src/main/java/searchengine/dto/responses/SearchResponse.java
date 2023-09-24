package searchengine.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SearchResponse {
    private Boolean result;
    private Integer count;
    private List<SearchDataResponse> data;
}
