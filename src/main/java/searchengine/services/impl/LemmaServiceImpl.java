package searchengine.services.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LemmaServiceImpl implements LemmaService {
    private final Map<String, Integer> lemmasInText = new HashMap<>();

    @Override
    public Map<String, Integer> getLemmasFromText(String html) throws IOException {
        String text = Jsoup.parse(html).text();
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> words = new ArrayList<>(List.of(text.replace(",", "").replace(".", "").toLowerCase().split(" ")));
        words.forEach(w -> determineLemma(w, luceneMorph));
        words.forEach(System.out::println);

        return lemmasInText;
    }

    private void determineLemma(String word, LuceneMorphology luceneMorphology) {
       if (String.valueOf(word.charAt(0)).matches("[a-z]") ||
            String.valueOf(word.charAt(0)).matches("[0-9]")) {
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
    }
}
