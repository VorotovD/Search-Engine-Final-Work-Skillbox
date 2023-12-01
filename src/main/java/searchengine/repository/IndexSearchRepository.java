package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexSearch;

import java.util.List;

@Repository
public interface IndexSearchRepository extends JpaRepository<IndexSearch, Integer> {
    @Query(value = "select i from IndexSearch i where i.pageId = :pageId and i.lemmaId = :lemmaId")
    IndexSearch indexSearchExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

    @Query(value = "select i from IndexSearch i where i.lemmaId = :lemmaId")
    List<IndexSearch> findIndexesByLemma(Integer lemmaId);

    @Query(value = "select i from IndexSearch i where i.pageId = :pageId")
    List<IndexSearch> findAllByPageId(@Param("pageId") Integer pageId);

    @Modifying
    @Transactional
    @Query(value = "delete from IndexSearch i where i.pageId = :pageId")
    void deleteAllByPageId(@Param("pageId") Integer pageId);
}
