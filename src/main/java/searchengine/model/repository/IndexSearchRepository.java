package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexSearch;

@Repository
public interface IndexSearchRepository extends JpaRepository<IndexSearch,Integer> {
}
