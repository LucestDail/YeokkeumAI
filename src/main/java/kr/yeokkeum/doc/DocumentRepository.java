package kr.yeokkeum.doc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
    /** 최신순 페이지 조회 — 전량 로드(OOM) 방지용. count()는 JpaRepository 기본 제공. */
    Page<Document> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
