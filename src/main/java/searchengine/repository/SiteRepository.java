package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SitePage;

@Repository
public interface SiteRepository extends JpaRepository<SitePage, Integer> {
    @Query(value = "select * from site s where s.url = :host limit 1", nativeQuery = true)
    SitePage getSitePageByUrl(@Param("host") String host);
}
