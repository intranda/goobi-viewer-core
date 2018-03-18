/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.toc.export.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.toc.TOCElement;

public class TocWriter {

    private static final Logger logger = LoggerFactory.getLogger(TocWriter.class);

    private static final int DEFAULT_LEVEL_INDENT = 20;
    private static final int TITLE_MARGIN = 30;
    private static final int TITLE_FONTSIZE = 20;
    private static final int ELEMENT_MARGIN = 10;

    private String title;
    private String author;
    private int levelIndent = DEFAULT_LEVEL_INDENT;

    public TocWriter(String author, String title) {
        this.author = author;
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public int getLevelIndent() {
        return levelIndent;
    }

    public void setLevelIndent(int levelIndent) {
        this.levelIndent = levelIndent;
    }

    public void createDocument(OutputStream output, List<TOCElement> elements) throws WriteTocException {

        Document document = new Document();
        try {

            PdfWriter.getInstance(document, output);
            document.addAuthor(getAuthor());
            document.addTitle(getTitle());
            document.addCreationDate();

            document.open();

            Paragraph titleParagraph = new Paragraph(getTitle());
            Font titleFont = titleParagraph.getFont();
            titleFont.setSize(TITLE_FONTSIZE);
            titleParagraph.setFont(titleFont);
            titleParagraph.setSpacingAfter(TITLE_MARGIN);
            document.add(titleParagraph);

            PdfPTable table = new PdfPTable(2);
            table.setWidths(new int[] { 10, 1 });
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            for (TOCElement TOCElement : elements) {
                Paragraph contentParagraph = new Paragraph(TOCElement.getLabel());
                contentParagraph.setIndentationLeft(getLevelIndent() * TOCElement.getLevel());
                PdfPCell contentCell = new PdfPCell();
                contentCell.setBorder(PdfPCell.NO_BORDER);
                contentCell.addElement(contentParagraph);
                contentCell.setPaddingBottom(ELEMENT_MARGIN);

                PdfPCell locationCell = new PdfPCell();
                locationCell.setBorder(PdfPCell.NO_BORDER);
                String location = TOCElement.getPageNoLabel();
                if (StringUtils.isNotBlank(location)) {
                    Paragraph locationParagraph = new Paragraph(location);
                    locationParagraph.setAlignment(Element.ALIGN_RIGHT);
                    locationCell.addElement(locationParagraph);
                }

                table.addCell(contentCell);
                table.addCell(locationCell);
            }

            document.add(table);

        } catch (DocumentException e) {
            throw new WriteTocException(e);
        } finally {
            document.close();
        }

    }

    public static void main(String[] args) throws FileNotFoundException, IOException, WriteTocException {
        File outputFile = new File("test.pdf");

        List<TOCElement> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(createRandomTOCElement());
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            TocWriter writer = new TocWriter("Its a me", "Inhaltsverzeichnis");
            writer.createDocument(fos, list);
        }
    }

    private static final String LOREM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer";
    private static final Random random = new Random();

    private static TOCElement createRandomTOCElement() {
        int level = random.nextInt(6);
        int startIndex = random.nextInt(100);
        int endIndex = startIndex + 1 + random.nextInt(199);
        String label = LOREM.substring(startIndex, endIndex).trim();
        String pageNo = Integer.toString(random.nextInt(9000) + 1);

        return new TOCElement(new SimpleMetadataValue(label), pageNo, pageNo, "1234", "LOG_0003", level, "PPNq234", "", true, false, "", null, null);
    }

}
