package kr.yeokkeum.doc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** 업로드 문서에서 텍스트 추출. PDF=PDFBox, txt/md=UTF-8. HWP/HWPX=로드맵(미지원). */
public final class DocText {

    private DocText() {}

    public static String extract(String filename, byte[] bytes) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(doc);
            } catch (IOException e) {
                throw new UncheckedIOException("PDF 파싱 실패", e);
            }
        }
        if (lower.endsWith(".hwp") || lower.endsWith(".hwpx")) {
            throw new UnsupportedOperationException(
                    "HWP/HWPX 파싱은 로드맵(현재 미지원). PDF 또는 텍스트로 변환해 업로드하세요.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
