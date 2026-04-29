/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.toc.export.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.toc.TOCElement;

/**
 * Writes a table-of-contents structure to a PDF document.
 */
public class TocWriter {

    private static final Logger logger = LogManager.getLogger(TocWriter.class); //NOSONAR Debug

    private static final int DEFAULT_LEVEL_INDENT = 20;
    private static final int TITLE_MARGIN = 30;
    private static final int TITLE_FONTSIZE = 20;
    private static final int ELEMENT_MARGIN = 10;

    private String title;
    private String author;
    private int levelIndent = DEFAULT_LEVEL_INDENT;

    /**
     * Creates a new TocWriter instance.
     *
     * @param author author name added to the PDF document metadata
     * @param title title displayed as heading and added to PDF metadata
     */
    public TocWriter(String author, String title) {
        this.author = author;
        this.title = title;
    }

    /**
     * Getter for the field <code>author</code>.
     *
     * @return the author of the document represented in this TOC entry
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Getter for the field <code>title</code>.
     *
     * @return the title of the document represented in this TOC entry
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter for the field <code>levelIndent</code>.
     *
     * @return a int.
     */
    public int getLevelIndent() {
        return levelIndent;
    }

    /**
     * Setter for the field <code>levelIndent</code>.
     *
     * @param levelIndent pixel indentation applied per hierarchy level
     */
    public void setLevelIndent(int levelIndent) {
        this.levelIndent = levelIndent;
    }

    /**
     * createDocument.
     *
     * @param output stream to write the generated PDF document to
     * @param elements TOC elements to render as PDF table rows
     * @throws io.goobi.viewer.model.toc.export.pdf.WriteTocException if any.
      * @should create pdf document from toc element list
     */
    public void createPdfDocument(OutputStream output, List<TOCElement> elements) throws WriteTocException {
        try (Document document = new Document()) {
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

            for (TOCElement element : elements) {
                Paragraph contentParagraph = new Paragraph(element.getLabel());
                contentParagraph.setIndentationLeft((float) getLevelIndent() * element.getLevel());
                PdfPCell contentCell = new PdfPCell();
                contentCell.setBorder(Rectangle.NO_BORDER);
                contentCell.addElement(contentParagraph);
                contentCell.setPaddingBottom(ELEMENT_MARGIN);

                PdfPCell locationCell = new PdfPCell();
                locationCell.setBorder(Rectangle.NO_BORDER);
                String location = element.getPageNoLabel();
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
        }
    }

    /**
     *
     * @param elements TOC element list
     * @return {@link String}
     */
    public String getAsText(List<TOCElement> elements) {
        StringBuilder sb = new StringBuilder();

        sb.append(getAuthor()).append('\n');
        sb.append(getTitle()).append("\n\n");

        for (TOCElement tocElement : elements) {
            for (int i = 0; i < getLevelIndent() * tocElement.getLevel(); ++i) {
                sb.append(' ');
            }
            sb.append(tocElement.getLabel());

            String location = tocElement.getPageNoLabel();
            if (StringUtils.isNotBlank(location)) {
                sb.append(" (").append(location).append(')');
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * main.
     *
     * @param args command-line arguments (unused)
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.model.toc.export.pdf.WriteTocException if any.
     */
    public static void main(String[] args) throws IOException, WriteTocException {
        File outputFile = new File("test.pdf");

        List<TOCElement> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(createRandomTOCElement());
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            TocWriter writer = new TocWriter("Its a me", "Inhaltsverzeichnis");
            writer.createPdfDocument(fos, list);
        }
    }

    private static final Random RANDOM = new SecureRandom();

    private static TOCElement createRandomTOCElement() {
        int level = RANDOM.nextInt(6);
        int startIndex = RANDOM.nextInt(100);
        int endIndex = startIndex + 1 + RANDOM.nextInt(199);
        String label = StringConstants.LOREM_IPSUM.substring(startIndex, endIndex).trim();
        String pageNo = Integer.toString(RANDOM.nextInt(9000) + 1);

        return new TOCElement(new SimpleMetadataValue(label), pageNo, pageNo, "1234", "LOG_0003", level, "PPNq234", "", true, false, true, "", null,
                null);
    }

}
