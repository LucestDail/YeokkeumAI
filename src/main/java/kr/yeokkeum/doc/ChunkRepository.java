package kr.yeokkeum.doc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChunkRepository extends JpaRepository<Chunk, String> {
    @Transactional
    void deleteByDocId(String docId);
}
