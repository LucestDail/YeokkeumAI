package kr.yeokkeum.doc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chunks")
public class Chunk {
    @Id
    private String id;
    private String docId;
    private String filename;
    private int idx;

    @Column(length = 1_000_000)
    private String text;

    protected Chunk() {}

    public Chunk(String id, String docId, String filename, int idx, String text) {
        this.id = id;
        this.docId = docId;
        this.filename = filename;
        this.idx = idx;
        this.text = text;
    }

    public String getId() { return id; }
    public String getDocId() { return docId; }
    public String getFilename() { return filename; }
    public int getIdx() { return idx; }
    public String getText() { return text; }
}
