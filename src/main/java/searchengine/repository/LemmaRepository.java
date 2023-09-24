package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma for update", nativeQuery = true)
    Lemma lemmaExist(@Param("lemma") String lemma);

    @Query(value = "select count(l) from Lemma l where l.siteId = :siteId")
    Integer findCountRecordBySiteId(Integer siteId);

    @Query(value = "select l.frequency from Lemma l where l.lemma = :lemma")
    Integer findCountPageByLemma(String lemma);

    @Query(value = "select l.id from Lemma l where l.lemma = :lemma")
    Integer findIdLemma(String lemma);
}
