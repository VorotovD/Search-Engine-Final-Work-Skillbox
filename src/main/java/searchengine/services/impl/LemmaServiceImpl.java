package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
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
    private LuceneMorphology luceneMorphology;

    {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Integer> getLemmasFromText(String html) {
        Map<String, Integer> lemmasInText = new HashMap<>();

        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.toLowerCase().split("[^a-zа-я]+")));
        words.forEach(w -> determineLemma(w, lemmasInText));
        return lemmasInText;
    }

    @Override
    public String getLemmaByWord(String word) {
        String preparedWord = word.toLowerCase();
        if (checkMatchWord(preparedWord)) return "";
        try {
            List<String> normalWordForms = luceneMorphology.getNormalForms(preparedWord);
            String wordInfo = luceneMorphology.getMorphInfo(preparedWord).toString();
            if (checkWordInfo(wordInfo)) return "";
            return normalWordForms.get(0);
        } catch (WrongCharaterException ex) {
            log.debug(ex.getMessage());
        }
        return "";
    }

    private void determineLemma(String word, Map<String, Integer> lemmasInText) {
        try {
            if (checkMatchWord(word)) {
                return;
            }
            List<String> normalWordForms = luceneMorphology.getNormalForms(word);
            String wordInfo = luceneMorphology.getMorphInfo(word).toString();
            if (checkWordInfo(wordInfo)) return;
            String normalWord = normalWordForms.get(0);
            lemmasInText.put(normalWord, lemmasInText.containsKey(normalWord) ? (lemmasInText.get(normalWord) + 1) : 1);
        } catch (RuntimeException ex) {
            log.debug(ex.getMessage());
        }
    }

    private boolean checkMatchWord(String word) {
        return word.isEmpty() || String.valueOf(word.charAt(0)).matches("[a-z]") || String.valueOf(word.charAt(0)).matches("[0-9]");
    }

    private boolean checkWordInfo(String wordInfo) {
        return wordInfo.contains("ПРЕДЛ") || wordInfo.contains("СОЮЗ") || wordInfo.contains("МЕЖД");
    }
}
