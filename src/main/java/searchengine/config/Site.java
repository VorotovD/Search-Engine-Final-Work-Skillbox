package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import java.net.URL;

@Setter
@Getter
public class Site {
    private URL url;
    private String name;
}
