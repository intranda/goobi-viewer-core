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
 * <p>
 * TocWriter class.
 * </p>
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
     * <p>
     * Constructor for TocWriter.
     * </p>
     *
     * @param author a {@link java.lang.String} object.
     * @param title a {@link java.lang.String} object.
     */
    public TocWriter(String author, String title) {
        this.author = author;
        this.title = title;
    }

    /**
     * <p>
     * Getter for the field <code>author</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>
     * Getter for the field <code>levelIndent</code>.
     * </p>
     *
     * @return a int.
     */
    public int getLevelIndent() {
        return levelIndent;
    }

    /**
     * <p>
     * Setter for the field <code>levelIndent</code>.
     * </p>
     *
     * @param levelIndent a int.
     */
    public void setLevelIndent(int levelIndent) {
        this.levelIndent = levelIndent;
    }

    /**
     * <p>
     * createDocument.
     * </p>
     *
     * @param output a {@link java.io.OutputStream} object.
     * @param elements a {@link java.util.List} object.
     * @throws io.goobi.viewer.model.toc.export.pdf.WriteTocException if any.
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
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
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
