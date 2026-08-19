package kr.yeokkeum.doc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * 업로드 문서에서 텍스트 추출. PDF=PDFBox, DOCX/XLSX=POI, txt/md=UTF-8.
 * HWP/HWPX는 {@link HwpExtractor}(rhwp)가 처리(여기선 방어적 거부).
 */
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
        if (lower.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                 XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return ex.getText();
            } catch (IOException e) {
                throw new UncheckedIOException("DOCX 파싱 실패", e);
            }
        }
        if (lower.endsWith(".xlsx")) {
            return extractXlsx(bytes);
        }
        if (lower.endsWith(".hwp") || lower.endsWith(".hwpx")) {
            throw new UnsupportedOperationException("HWP/HWPX 는 HwpExtractor(rhwp) 경로로 처리해야 합니다.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String extractXlsx(byte[] bytes) {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            for (Sheet sheet : wb) {
                sb.append("# ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        if (line.length() > 0) line.append('\t');
                        line.append(cellText(cell));
                    }
                    if (line.length() > 0) sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("XLSX 파싱 실패", e);
        }
    }

    private static String cellText(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
