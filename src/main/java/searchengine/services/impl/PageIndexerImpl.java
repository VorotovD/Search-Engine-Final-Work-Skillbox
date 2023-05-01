package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class PageIndexerImpl implements PageIndexer {
    private LemmaService lemmaService;
    private LemmaRepository lemmaRepository;
    private IndexSearchRepository indexSearchRepository;

    @Override
    public void indexHtml(String html, Page indexingPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> {
                saveLemma(entry.getKey(),entry.getValue(),indexingPage);
            });
//            lemmas.forEach((k, v) -> {
//                saveLemma(k,v,indexingPage);
//            });
            log.warn("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка сохранения леммы: ", e);
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }

    }

    @Transactional
    private void saveLemma(String k,Integer v, Page indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            long nu = System.currentTimeMillis();
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, v);
        } else {
            Lemma newLemmaToDB = new Lemma();
            newLemmaToDB.setSiteId(indexingPage.getSiteId());
            newLemmaToDB.setLemma(k);
            newLemmaToDB.setFrequency(v);
            newLemmaToDB.setSitePage(indexingPage.getSitePage());
            //todo возможна ошибка какая? если есть такая запись отловить
            lemmaRepository.saveAndFlush(newLemmaToDB);
            createIndex(indexingPage, newLemmaToDB, v);
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
