package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {
    private LemmaService lemmaService;
    private LemmaRepository lemmaRepository;
    private IndexSearchRepository indexSearchRepository;

    @Override
    public void indexHtml(String html, Page indexingPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.warn("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refreshIndex(String html, Page refreshPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> refreshLemma(entry.getKey(), entry.getValue(), refreshPage));
            log.warn("Обновление индекса страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    @Transactional
    private void refreshLemma(String k, Integer v, Page refreshPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            IndexSearch indexToRefresh = indexSearchRepository.indexSearchExist(refreshPage.getId(), existLemmaInDB.getId());
            if (indexToRefresh != null) {
                existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() - indexToRefresh.getLemmaCount());
                lemmaRepository.saveAndFlush(existLemmaInDB);
                indexSearchRepository.delete(indexToRefresh);
                Lemma newLemmaToDB = new Lemma();
                newLemmaToDB.setSiteId(refreshPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSitePage(refreshPage.getSitePage());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(refreshPage, newLemmaToDB, v);
                return;
            }
        }
        Lemma newLemmaToDB = new Lemma();
        newLemmaToDB.setSiteId(refreshPage.getSiteId());
        newLemmaToDB.setLemma(k);
        newLemmaToDB.setFrequency(v);
        newLemmaToDB.setSitePage(refreshPage.getSitePage());
        lemmaRepository.saveAndFlush(newLemmaToDB);
        createIndex(refreshPage, newLemmaToDB, v);
    }

    @Transactional
    private void saveLemma(String k, Integer v, Page indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, v);
        } else {
            try {
                Lemma newLemmaToDB = new Lemma();
                newLemmaToDB.setSiteId(indexingPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSitePage(indexingPage.getSitePage());
                //todo возможна ошибка какая? если есть такая запись отловить
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, v);
            } catch (DataIntegrityViolationException ex) {
                log.error("Ошибка при сохранении леммы, такая лемма уже существует. Вызов повторного сохранения");
                saveLemma(k, v, indexingPage);
            }
        }
    }

    private void createIndex(Page indexingPage, Lemma lemmaInDB, Integer rank) {
        IndexSearch indexSearchExist = indexSearchRepository.indexSearchExist(indexingPage.getId(), lemmaInDB.getId());
        if (indexSearchExist != null) {
            indexSearchExist.setLemmaCount(indexSearchExist.getLemmaCount() + rank);
            indexSearchRepository.save(indexSearchExist);
        } else {
            IndexSearch index = new IndexSearch();
            index.setPageId(indexingPage.getId());
            index.setLemmaId(lemmaInDB.getId());
            index.setLemmaCount(rank);
            index.setLemma(lemmaInDB);
            index.setPage(indexingPage);
            indexSearchRepository.save(index);
        }
    }
}
