package searchengine.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NotOkResponse {
    private final Boolean result = false;
    private String error;
}
