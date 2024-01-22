package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {
    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;

    @Override
    public void indexHtml(String html, Page indexingPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.debug("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
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
            //уменьшение frequency у лемм которые присутствуют на обновляемой странице
            refreshLemma(refreshPage);
            //удаление индекса
            indexSearchRepository.deleteAllByPageId(refreshPage.getId());
            //обновление лемм и индесов у обнолвенной страницы
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), refreshPage));
            log.debug("Обновление индекса страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    @Transactional
    private void refreshLemma(Page refreshPage) {
        List<IndexSearch> indexes = indexSearchRepository.findAllByPageId(refreshPage.getId());
        indexes.forEach(idx -> {
            Optional<Lemma> lemmaToRefresh = lemmaRepository.findById(idx.getLemmaId());
            lemmaToRefresh.ifPresent(lemma -> {
                lemma.setFrequency(lemma.getFrequency() - idx.getLemmaCount());
                lemmaRepository.saveAndFlush(lemma);
            });
        });
    }

    @Transactional
    private void saveLemma(String k, Integer v, Page indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(k, indexingPage.getSiteId());
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
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, v);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Ошибка при сохранении леммы, такая лемма уже существует. Вызов повторного сохранения");
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
