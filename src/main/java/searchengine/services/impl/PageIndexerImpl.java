package searchengine.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.util.Map;

@Service
public class PageIndexerImpl implements PageIndexer {
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexSearchRepository indexSearchRepository;
    private static final Logger logger = LoggerFactory.getLogger(PageIndexerImpl.class);
    @Override
    public void indexHtml(String html, Page indexingPage) {
        try {
            Map<String,Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.forEach((k,v) -> {
                Lemma existLemmaInDB = lemmaRepository.lemmaExist(k);
                if (existLemmaInDB != null){
                    existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
                    lemmaRepository.saveAndFlush(existLemmaInDB);
                    createIndex(indexingPage,existLemmaInDB);
                } else {
                    Lemma newLemmaToDB = new Lemma();
                    newLemmaToDB.setSiteId(indexingPage.getSiteId());
                    newLemmaToDB.setLemma(k);
                    newLemmaToDB.setFrequency(v);
                    newLemmaToDB.setSitePage(indexingPage.getSitePage());
                    //возможна ошибка какая? если есть такая запись отловить
                    lemmaRepository.saveAndFlush(newLemmaToDB);
                    createIndex(indexingPage,newLemmaToDB);
                }
            });
        } catch (Exception ex) {
            logger.error("Ошибка сохранения леммы: ",ex);
        }
    }

    private void createIndex(Page indexingPage, Lemma lemma) {
        IndexSearch indexSearchExist = indexSearchRepository.indexSearchExist(indexingPage.getId(),lemma.getId());
        if (indexSearchExist != null) {
            indexSearchExist.setLemmaCount(lemma.getFrequency());
            indexSearchRepository.save(indexSearchExist);
        } else {
            IndexSearch index = new IndexSearch();
            index.setPageId(indexingPage.getId());
           index.setLemmaId(lemma.getId());
            index.setLemmaCount(lemma.getFrequency());
            index.setLemma(lemma);
            index.setPage(indexingPage);
            indexSearchRepository.save(index);
        }
    }
}
