package kr.yeokkeum.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 감사로그 — 누가·언제·무엇을·어떤 근거로(HITL/책임성). */
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    private String id;
    private long ts;
    private String actor;
    private String role;
    private String action;

    @Column(length = 1_000_000)
    private String detail;

    protected AuditLog() {}

    public AuditLog(String id, long ts, String actor, String role, String action, String detail) {
        this.id = id;
        this.ts = ts;
        this.actor = actor;
        this.role = role;
        this.action = action;
        this.detail = detail;
    }

    public String getId() { return id; }
    public long getTs() { return ts; }
    public String getActor() { return actor; }
    public String getRole() { return role; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
