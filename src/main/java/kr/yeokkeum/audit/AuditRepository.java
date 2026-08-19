package kr.yeokkeum.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findAllByOrderByTsDesc(Pageable pageable);

    /** 필터 검색 — action/actor 는 null 이면 무시, ts 범위 [from,to]. */
    @Query("select a from AuditLog a where (:action is null or a.action = :action) "
            + "and (:actor is null or a.actor = :actor) and a.ts >= :from and a.ts <= :to order by a.ts desc")
    List<AuditLog> search(@Param("action") String action, @Param("actor") String actor,
                          @Param("from") long from, @Param("to") long to, Pageable pageable);
}
