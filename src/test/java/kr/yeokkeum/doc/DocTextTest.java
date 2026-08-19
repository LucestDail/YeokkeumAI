package kr.yeokkeum.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocTextTest {

    @Test
    void extractsDocx() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("규정검토 대상 문서입니다");
            doc.write(bos);
        }
        assertThat(DocText.extract("a.docx", bos.toByteArray())).contains("규정검토 대상 문서");
    }

    @Test
    void extractsXlsx() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var row = wb.createSheet("시트1").createRow(0);
            row.createCell(0).setCellValue("항목");
            row.createCell(1).setCellValue("값");
            wb.write(bos);
        }
        assertThat(DocText.extract("a.xlsx", bos.toByteArray())).contains("항목").contains("값");
    }

    @Test
    void textPassthrough() {
        assertThat(DocText.extract("x.txt", "안녕 텍스트".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("안녕 텍스트");
    }
}
