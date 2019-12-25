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
package io.goobi.viewer.controller;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.docx4j.Docx4J;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * <p>ConversionTools class.</p>
 *
 */
public class ConversionTools {

    private static final Logger logger = LoggerFactory.getLogger(ConversionTools.class);

    /**
     * Converts given file to HTML, if supported by Tika.
     *
     * @param file a {@link java.nio.file.Path} object.
     * @return String containing the HTML
     * @throws @throws FileNotFoundException
     * @should convert docx file correctly
     * @should convert rtf file correctly
     */
    public static String convertFileToHtml(Path file) throws IOException {
        String ret = null;

        ContentHandler handler = new ToXMLContentHandler();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(file.toFile())) {
            parser.parse(stream, handler, metadata);
            ret = handler.toString();
            // Remove bad tags
            ret = ret.replaceAll("<b />", "").replace("<b/>", "");
        } catch (TikaException e) {
            logger.error(e.getMessage(), e);
        } catch (SAXException e) {
            logger.error(e.getMessage(), e);
        }
        //        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        //        AutoDetectParser parser = new AutoDetectParser();
        //        Metadata metadata = new Metadata();
        //        try (InputStream stream = new FileInputStream(file.toFile()); StringWriter sw = new StringWriter()) {
        //            TransformerHandler handler = factory.newTransformerHandler();
        //            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
        //            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        //            handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        //            handler.setResult(new StreamResult(sw));
        //
        //            parser.parse(stream, handler, metadata, new ParseContext());
        //
        //            return sw.toString();
        //        } catch (TransformerConfigurationException e) {
        //            logger.error(e.getMessage(), e);
        //        } catch (SAXException e) {
        //            logger.error(e.getMessage(), e);
        //        } catch (TikaException e) {
        //            logger.error(e.getMessage(), e);
        //        }

        return ret;
    }

    /**
     * Converts given DOCX file to HTML using DOCX4J (usually with better results than using Tika).
     *
     * @param file a {@link java.nio.file.Path} object.
     * @return String containing the HTML
     * @throws java.io.IOException
     * @should convert docx correctly
     */
    public static String convertDocxToHtml(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        WordprocessingMLPackage wordMLPackage = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            wordMLPackage = Docx4J.load(file.toFile());

            HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
            htmlSettings.setImageDirPath(file.toAbsolutePath().toString() + "_files");
            htmlSettings
                    .setImageTargetUri(file.toAbsolutePath().toString().substring(file.toAbsolutePath().toString().lastIndexOf("/") + 1) + "_files");
            htmlSettings.setWmlPackage(wordMLPackage);

            Docx4J.toHTML(htmlSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);

            return os.toString(Helper.DEFAULT_ENCODING);
        } catch (Docx4JException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (wordMLPackage != null && wordMLPackage.getMainDocumentPart().getFontTablePart() != null) {
                wordMLPackage.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
            }
        }

        return null;
    }
}
