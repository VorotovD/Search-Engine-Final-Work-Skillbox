package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LemmaServiceImpl implements LemmaService {

    @Override
    public Map<String, Integer> getLemmasFromText(String html) throws IOException {
        Map<String, Integer> lemmasInText = new HashMap<>();
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.toLowerCase().split("[^a-zа-я]+")));
        words.forEach(w -> determineLemma(w, luceneMorph, lemmasInText));
        return lemmasInText;
    }

    private void determineLemma(String word, LuceneMorphology luceneMorphology, Map<String, Integer> lemmasInText) {
        try {
            if (word.isEmpty() || String.valueOf(word.charAt(0)).matches("[a-z]") || String.valueOf(word.charAt(0)).matches("[0-9]")) {
                return;
            }
            List<String> normalWordForms = luceneMorphology.getNormalForms(word);
            String wordInfo = luceneMorphology.getMorphInfo(word).toString();
            if (wordInfo.contains("ПРЕДЛ") || wordInfo.contains("СОЮЗ") || wordInfo.contains("МЕЖД")) {
                return;
            }
            String normalWord = normalWordForms.get(0);
            lemmasInText.put(normalWord, lemmasInText.containsKey(normalWord) ? (lemmasInText.get(normalWord) + 1) : 1);
        } catch (RuntimeException ex) {
            log.debug(ex.getMessage());
        }

    }
}
