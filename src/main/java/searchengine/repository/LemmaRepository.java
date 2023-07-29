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

    @Query(value = "select count(*) from lemma l where l.site_id = :siteId", nativeQuery = true)
    Integer findCountRecordBySiteId(@Param("siteId") Integer siteId);
}
