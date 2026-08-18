package kr.yeokkeum.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findAllByOrderByTsDesc(Pageable pageable);
}
