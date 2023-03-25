package searchengine.services.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LemmaServiceImpl implements LemmaService {
    //TODO не понятно что будет при многопоточности, будут ли путаться мапы
    private final Map<String, Integer> lemmasInText = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(LemmaServiceImpl.class);

    @Override
    public Map<String, Integer> getLemmasFromText(String html) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.replaceAll("(?U)\\pP","").toLowerCase().split(" ")));
        words.forEach(w -> determineLemma(w, luceneMorph));
        return lemmasInText;
    }

    private void determineLemma(String word, LuceneMorphology luceneMorphology) {
        try{
            if (word.isEmpty() || String.valueOf(word.charAt(0)).matches("[a-z]") || String.valueOf(word.charAt(0)).matches("[0-9]")) {
                return;
            }
            List<String> normalWordForms = luceneMorphology.getNormalForms(word);
            String wordInfo = luceneMorphology.getMorphInfo(word).toString();
            if (wordInfo.contains("ПРЕДЛ") || wordInfo.contains("СОЮЗ") || wordInfo.contains("МЕЖД")) {
                return;
            }
            normalWordForms.forEach(w -> {
                if (!lemmasInText.containsKey(w)) {
                    lemmasInText.put(w,1);
                } else {
                    lemmasInText.replace(w,lemmasInText.get(w) + 1);
                }
            });
        } catch (RuntimeException ex) {
            //todo раскоментировать для получения информации о немечатных символах
            //logger.debug(ex.getMessage());
        }

    }

    @Override
    public void getLemmasFromUrl(URL url) throws IOException {
        org.jsoup.Connection connect = Jsoup.connect(String.valueOf(url));
        Document doc = connect.timeout(60000).get();
        Map<String,Integer> res = getLemmasFromText(doc.body().html());
        System.out.println(res.keySet());
        System.out.println(res.values());
    }
}