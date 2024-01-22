package searchengine.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.Page;

@Getter
@Setter
@NoArgsConstructor
public class RankDto {
    private Integer pageId;
    private Page page;
    private double absRelevance = 0.0;
    private double relativeRelevance = 0.0;
    private int maxLemmaRank = 0;
}
