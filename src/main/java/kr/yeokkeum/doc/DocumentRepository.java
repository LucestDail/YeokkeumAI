package kr.yeokkeum.doc;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findAllByOrderByCreatedAtDesc();
}
