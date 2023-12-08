package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.RankDto;
import searchengine.dto.responses.NotOkResponse;
import searchengine.dto.responses.SearchDataResponse;
import searchengine.dto.responses.SearchResponse;
import searchengine.model.IndexSearch;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexRepository;
    private final LemmaService lemmaService;
    private final Status indexSuccessStatus = Status.INDEXED;
    private final double frequencyLimitProportion = 100;

    @Override
    public ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit) throws IOException {
        if (checkIndexStatusNotIndexed(site)) {
            return ResponseEntity.badRequest().body(new NotOkResponse("Индексация сайта для поиска не закончена"));
        }
        Integer countPages = pageRepository.getCountPages();
        log.info("Total pages:" + countPages.toString());

        //Exclusion lemmas by frequent
        List<String> lemmasForSearch = new ArrayList<>(lemmaService.getLemmasFromText(query).keySet());
        lemmasForSearch.removeIf(e -> {
            Integer lemmaFrequency = lemmaRepository.findCountPageByLemma(e);
            if (lemmaFrequency == null) return true;
            log.info("Lemma frequency(" + e + "):" + lemmaFrequency);
            log.info("Frequency limit:" + (double) lemmaFrequency / countPages);
            return ((double) lemmaFrequency / countPages > frequencyLimitProportion);
        });

        if (lemmasForSearch.isEmpty()) {
            return ResponseEntity.badRequest().body(new NotOkResponse("Нет ни одного совпадения по заданному поисковому запросу."));
        }

        //Sorting lemmas by frequent
        List<AbstractMap.SimpleEntry<Integer, String>> lemmasFrequencyToSearch = lemmasForSearch.
                stream().
                map(l -> new AbstractMap.SimpleEntry<>(lemmaRepository.findCountPageByLemma(l), l)).toList();
        List<String> sortedLemmasToSearch = lemmasFrequencyToSearch.
                stream().
                sorted(Comparator.comparingInt(Map.Entry::getKey)).
                map(Map.Entry::getValue).
                toList();
        log.info(sortedLemmasToSearch.toString());

        //Search pages by first lemma
        List<IndexSearch> resultOfSearch = indexRepository.findIndexesByLemma(lemmaRepository.findIdLemma(sortedLemmasToSearch.get(0)));
        for (int i = 1; i <= sortedLemmasToSearch.size() - 1; i++) {
            List<IndexSearch> samePages = new ArrayList<>();
            List<IndexSearch> pagesNextLemma = indexRepository.findIndexesByLemma(lemmaRepository.findIdLemma(sortedLemmasToSearch.get(i)));
            List<IndexSearch> finalResultOfSearch = resultOfSearch;

            pagesNextLemma.forEach(index -> {
                for (IndexSearch res : finalResultOfSearch) {
                    if (res.getPageId() == index.getPageId()) {
                        samePages.add(res);
                        samePages.add(index);
                    }
                }
            });
            resultOfSearch = samePages;
        }

        //Output empty result
        if (resultOfSearch.isEmpty()) {
            return ResponseEntity.ok().body(new SearchResponse(true, 0, Collections.emptyList()));
        }

        //Rank calculation
        Set<RankDto> pagesRelevance = new HashSet<>();
        resultOfSearch = resultOfSearch.stream().sorted(Comparator.comparingInt(IndexSearch::getPageId)).collect(Collectors.toList());
        int pageId = resultOfSearch.get(0).getPageId();
        RankDto rankPage = new RankDto();
        for (IndexSearch index : resultOfSearch) {
            if (index.getPageId() == pageId) {
                rankPage.setPageId(index.getPageId());
                rankPage.setAbsRelevance(rankPage.getAbsRelevance() + index.getLemmaCount());
                if (rankPage.getMaxLemmaRank() < index.getLemmaCount()) rankPage.setMaxLemmaRank(index.getLemmaCount());
            } else {
                rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
                pagesRelevance.add(rankPage);
                rankPage = new RankDto();
                pageId = index.getPageId();
                rankPage.setPageId(index.getPageId());
                rankPage.setAbsRelevance(rankPage.getAbsRelevance() + index.getLemmaCount());
                if (rankPage.getMaxLemmaRank() < index.getLemmaCount()) rankPage.setMaxLemmaRank(index.getLemmaCount());
            }
        }
        rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
        pagesRelevance.add(rankPage);

        //Sort pages Relevance
        LinkedHashSet<RankDto> pagesRelevanceSorted = pagesRelevance.stream().sorted(Comparator.comparingDouble(RankDto::getRelativeRelevance)).collect(Collectors.toCollection(LinkedHashSet::new));

        //Converting pages relevance to searchDataResponses
        LinkedList<SearchDataResponse> searchDataResponses = new LinkedList<>();
        for (RankDto rank : pagesRelevanceSorted) {
            AtomicReference<Page> page = new AtomicReference<>(new Page());
            pageRepository.findById(rank.getPageId()).ifPresent(p -> page.set(new Page(p)));
            SitePage sitePage = siteRepository.findById(pageRepository.findById(rank.getPageId()).get().getSiteId()).get();
            Document doc = Jsoup.parse(page.get().getContent());

            searchDataResponses.add(new SearchDataResponse(
                    sitePage.getUrl(),
                    sitePage.getName(),
                    page.get().getPath(),
                    doc.title(),
                    getSnipped(doc),
                    rank.getRelativeRelevance()
            ));
        }

        return ResponseEntity.ok(searchDataResponses);
    }

    private String getSnipped(Document doc) {
        //todo достать все тексты из тегов,
        // каждый пропустить через лемматизатор,
        // сравнить запрос пользователя и леммы из текста,
        // если все совпало - вывести


        return "SNIPPET";
    }

    private Boolean checkIndexStatusNotIndexed(String site) {
        if (site == null || site.isBlank()) {
            List<SitePage> sites = siteRepository.findAll();
            return sites.stream().anyMatch(s -> !s.getStatus().equals(indexSuccessStatus));
        }
        return !siteRepository.getSitePageByUrl(site).getStatus().equals(indexSuccessStatus);
    }
}
