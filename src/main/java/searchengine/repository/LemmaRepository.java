package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma and t.site_id = :siteId for update", nativeQuery = true)
    Lemma lemmaExist(String lemma, Integer siteId);

    @Query(value = "select count(l) from Lemma l where l.siteId = :siteId")
    Integer findCountRecordBySiteId(Integer siteId);

    @Query(value = "select l.frequency from Lemma l where l.lemma = :lemma and (:siteId is null or l.siteId = :siteId)")
    Integer findCountPageByLemma(String lemma, Integer siteId);

    @Query(value = "select l.id from Lemma l where l.lemma = :lemma")
    Integer findIdLemma(String lemma);

    @Query(value = "select l from Lemma l where l.lemma = :lemma and (:siteId is null or l.siteId = :siteId)")
    List<Lemma> findLemmasByLemmaAndSiteId(String lemma, Integer siteId);
}
