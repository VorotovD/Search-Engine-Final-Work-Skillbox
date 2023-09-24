package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexSearch;

import java.util.List;

@Repository
public interface IndexSearchRepository extends JpaRepository<IndexSearch, Integer> {
    @Query(value = "select * from index_search t where t.page_id = :pageId and t.lemma_id = :lemmaId", nativeQuery = true)
    IndexSearch indexSearchExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);
    @Query(value = "select i from IndexSearch i where i.lemmaId = :lemmaId")
    List<IndexSearch> findIndexesByLemma(Integer lemmaId);
}
