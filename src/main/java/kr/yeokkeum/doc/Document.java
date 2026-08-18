package kr.yeokkeum.doc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    private String id;
    private String filename;
    private int chars;
    private int nChunks;
    private long createdAt;

    protected Document() {}

    public Document(String id, String filename, int chars, int nChunks, long createdAt) {
        this.id = id;
        this.filename = filename;
        this.chars = chars;
        this.nChunks = nChunks;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getFilename() { return filename; }
    public int getChars() { return chars; }
    public int getNChunks() { return nChunks; }
    public long getCreatedAt() { return createdAt; }
}
