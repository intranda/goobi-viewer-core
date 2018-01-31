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
package de.intranda.digiverso.presentation.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertAbbyyToAlto {

    private static final Logger logger = LoggerFactory.getLogger(ConvertAbbyyToAlto.class);

    //	private static Namespace altoNamespace = Namespace.getNamespace("alto", "http://www.loc.gov/standards/alto/ns-v2#");
    private static Namespace defaultNamespace = Namespace.getNamespace("http://www.loc.gov/standards/alto/ns-v2#");
    private static Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    //	private static Namespace abbyyNamespace = Namespace.getNamespace("http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml");
    private static Namespace abbyyNamespace = Namespace.getNamespace("http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml");

    private Element abbyy;
    private String inputfilename;
    private Date creationtime;

    private int pageCount = 0;
    private int textBlockCount = 0;
    private int textLineCount = 0;
    private int stringCount = 0;

    private int confidenceTotal = 0;
    private int characterCount = 0;

    private int pageBlockCount = 0;

    public Element convert(File input) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        creationtime = new Date(input.lastModified());
        Document abbyyDoc = builder.build(input);
        inputfilename = input.getName();
        abbyy = abbyyDoc.getRootElement();

        Element alto = addAltoHeader();
        alto = addLayout(alto);
        //		addAltoConfidence(alto.getChild("Description", altoNamespace).getChild("OCRProcessing", altoNamespace)
        //				.getChild("preProcessingStep", altoNamespace));
        return alto;
    }

    public Element convert(Document abbyyDoc) {
        abbyy = abbyyDoc.getRootElement();
        Element alto = addAltoHeader();
        alto = addLayout(alto);
        return alto;
    }

    /**
     * Add ALTO Layout Element
     */
    private Element addLayout(Element alto) {
        Element layout = new Element("Layout", defaultNamespace);
        alto.addContent(layout);

        List<Element> abbyypages = abbyy.getChildren("page", abbyyNamespace);
        for (Element abbyypage : abbyypages) {
            pageCount++;
            String height = abbyypage.getAttributeValue("height");
            String width = abbyypage.getAttributeValue("width");

            Element altopage = generatePage("Page_" + pageCount, height, width, pageCount);
            layout.addContent(altopage);
            addPrintSpace(abbyypage, altopage);
        }

        return alto;

    }

    private void addPrintSpace(Element abbyypage, Element altopage) {
        List<Element> abbyyPageBlocks = abbyypage.getChildren("block", abbyyNamespace);

        int l = 0;
        int t = 0;
        int r = 0;
        int b = 0;
        Element printSpace = new Element("PrintSpace", defaultNamespace);

        for (Element abbyyPageBlock : abbyyPageBlocks) {
            pageBlockCount++;
            if (Integer.valueOf(abbyyPageBlock.getAttributeValue("l")) < l || l == 0) {
                l = Integer.valueOf(abbyyPageBlock.getAttributeValue("l"));
            }

            if (Integer.valueOf(abbyyPageBlock.getAttributeValue("t")) < t || t == 0) {
                t = Integer.valueOf(abbyyPageBlock.getAttributeValue("t"));
            }

            if (Integer.valueOf(abbyyPageBlock.getAttributeValue("r")) > r || r == 0) {
                r = Integer.valueOf(abbyyPageBlock.getAttributeValue("r"));
            }

            if (Integer.valueOf(abbyyPageBlock.getAttributeValue("b")) > b || b == 0) {
                b = Integer.valueOf(abbyyPageBlock.getAttributeValue("b"));
            }

            int hpos = l;
            int vpos = t;
            int height = b - t;
            int width = r - l;

            if (abbyyPageBlock.getAttributeValue("blockType").equals("Text")) {
                Element altoTextBlock = new Element("TextBlock", defaultNamespace);
                printSpace.addContent(altoTextBlock);
                altoTextBlock.setAttribute("ID", "TextBlock_" + textBlockCount);
                altoTextBlock.setAttribute("HPOS", Integer.toString(hpos));
                altoTextBlock.setAttribute("VPOS", Integer.toString(vpos));
                altoTextBlock.setAttribute("HEIGHT", Integer.toString(height));
                altoTextBlock.setAttribute("WIDTH", Integer.toString(width));

                addTextBlocks(altoTextBlock, abbyyPageBlock);
            } else if (abbyyPageBlock.getAttributeValue(("blockType")).equals("Table")) {
                Element altoTextBlock = new Element("TextBlock", defaultNamespace);
                printSpace.addContent(altoTextBlock);
                altoTextBlock.setAttribute("ID", "TextBlock_" + textBlockCount);
                altoTextBlock.setAttribute("HPOS", Integer.toString(hpos));
                altoTextBlock.setAttribute("VPOS", Integer.toString(vpos));
                altoTextBlock.setAttribute("HEIGHT", Integer.toString(height));
                altoTextBlock.setAttribute("WIDTH", Integer.toString(width));

                addTableBlocks(altoTextBlock, abbyyPageBlock);
            }
        }
        int hpos = l;
        int vpos = t;
        int height = b - t;
        int width = r - l;
        printSpace.setAttribute("ID", "PrintSpace_" + pageBlockCount);
        printSpace.setAttribute("HPOS", Integer.toString(hpos));
        printSpace.setAttribute("VPOS", Integer.toString(vpos));
        printSpace.setAttribute("HEIGHT", Integer.toString(height));
        printSpace.setAttribute("WIDTH", Integer.toString(width));
        altopage.addContent(printSpace);
    }

    private void addTextBlocks(Element altoTextBlock, Element abbyyPageBlock) {
        List<Element> abbyyPars = new ArrayList<>();
        for (Element text : abbyyPageBlock.getChildren("text", abbyyNamespace)) {
            List<Element> pairs = text.getChildren("par", abbyyNamespace);
            {
                for (Element pair : pairs) {
                    abbyyPars.add(pair);
                }
            }
        }

        for (Element abbyyPar : abbyyPars) {
            textBlockCount++;
            int l = 0;
            int t = 0;
            int r = 0;
            int b = 0;
            List<Element> charParamsList = getChildren(abbyyPar);

            for (Element charParams : charParamsList) {
                if (Integer.valueOf(charParams.getAttributeValue("l")) < l || l == 0) {
                    l = Integer.valueOf(charParams.getAttributeValue("l"));
                }
                if (Integer.valueOf(charParams.getAttributeValue("t")) < t || t == 0) {
                    t = Integer.valueOf(charParams.getAttributeValue("t"));
                }

                if (Integer.valueOf(charParams.getAttributeValue("r")) > r || r == 0) {
                    r = Integer.valueOf(charParams.getAttributeValue("r"));
                }
                if (Integer.valueOf(charParams.getAttributeValue("b")) > b || b == 0) {
                    b = Integer.valueOf(charParams.getAttributeValue(""));
                }
            }
            //			int hpos = l;
            //			int vpos = t;
            //			int height = b - t;
            //			int width = r - l;

            addTextLines(altoTextBlock, abbyyPar);
        }
    }

    private void addTableBlocks(Element altoTextBlock, Element abbyyPageBlock) {

        List<Element> abbyyPars = new ArrayList<>();
        List<Element> abbyyRows = abbyyPageBlock.getChildren("row", abbyyNamespace);
        for (Element abbyyRow : abbyyRows) {
            List<Element> abbyyCells = abbyyRow.getChildren("cell", abbyyNamespace);
            for (Element abbyyCell : abbyyCells) {
                List<Element> abbyytexts = abbyyCell.getChildren("text", abbyyNamespace);
                for (Element text : abbyytexts) {
                    List<Element> pairs = text.getChildren("par", abbyyNamespace);
                    {
                        for (Element pair : pairs) {
                            abbyyPars.add(pair);
                        }
                    }
                }
            }
        }

        for (Element abbyyPar : abbyyPars) {
            textBlockCount++;
            int l = 0;
            int t = 0;
            int r = 0;
            int b = 0;
            List<Element> charParamsList = getChildren(abbyyPar);

            for (Element charParams : charParamsList) {
                if (Integer.valueOf(charParams.getAttributeValue("l")) < l || l == 0) {
                    l = Integer.valueOf(charParams.getAttributeValue("l"));
                }
                if (Integer.valueOf(charParams.getAttributeValue("t")) < t || t == 0) {
                    t = Integer.valueOf(charParams.getAttributeValue("t"));
                }

                if (Integer.valueOf(charParams.getAttributeValue("r")) > r || r == 0) {
                    r = Integer.valueOf(charParams.getAttributeValue("r"));
                }
                if (Integer.valueOf(charParams.getAttributeValue("b")) > b || b == 0) {
                    b = Integer.valueOf(charParams.getAttributeValue("b"));
                }
            }
            //          int hpos = l;
            //          int vpos = t;
            //          int height = b - t;
            //          int width = r - l;

            addTextLines(altoTextBlock, abbyyPar);
        }
    }

    private void addTextLines(Element altoTextBlock, Element abbyyPar) {
        List<Element> abbyyLines = abbyyPar.getChildren("line", abbyyNamespace);
        int l = 0;
        int t = 0;
        int r = 0;
        int b = 0;
        for (Element abbyyLine : abbyyLines) {
            textLineCount++;
            List<Element> charParamsList = getLineChildren(abbyyLine);
            if (!charParamsList.isEmpty()) {
                Element first = charParamsList.get(0);
                Element last = charParamsList.get(charParamsList.size() - 1);

                l = Integer.valueOf(first.getAttributeValue("l"));
                t = Integer.valueOf(first.getAttributeValue("t"));
                r = Integer.valueOf(last.getAttributeValue("r"));
                b = Integer.valueOf(last.getAttributeValue("b"));

                for (Element charParams : charParamsList) {
                    if (Integer.valueOf(charParams.getAttributeValue("l")) < l || l == 0) {
                        l = Integer.valueOf(charParams.getAttributeValue("l"));
                    }
                    if (Integer.valueOf(charParams.getAttributeValue("t")) < t || t == 0) {
                        t = Integer.valueOf(charParams.getAttributeValue("t"));
                    }
                    if (Integer.valueOf(charParams.getAttributeValue("r")) > r || r == 0) {
                        r = Integer.valueOf(charParams.getAttributeValue("r"));
                    }
                    if (Integer.valueOf(charParams.getAttributeValue("b")) > b || b == 0) {
                        b = Integer.valueOf(charParams.getAttributeValue("b"));
                    }
                }
            } else { //get extent from line attributes
                try {
                    l = Integer.valueOf(abbyyLine.getAttributeValue("l"));
                    t = Integer.valueOf(abbyyLine.getAttributeValue("t"));
                    r = Integer.valueOf(abbyyLine.getAttributeValue("r"));
                    b = Integer.valueOf(abbyyLine.getAttributeValue("b"));
                } catch (NullPointerException e) {
                    logger.error("Unable to extract line coordinates from abbyy");
                    l = t = r = b = 0;
                }
            }

            int hpos = l;
            int vpos = t;
            int height = b - t;
            int width = r - l;

            Element altoTextLine = new Element("TextLine", defaultNamespace);
            altoTextBlock.addContent(altoTextLine);
            altoTextLine.setAttribute("ID", "TextLine_" + textLineCount);
            altoTextLine.setAttribute("HPOS", Integer.toString(hpos));
            altoTextLine.setAttribute("VPOS", Integer.toString(vpos));
            altoTextLine.setAttribute("HEIGHT", Integer.toString(height));
            altoTextLine.setAttribute("WIDTH", Integer.toString(width));

            //				addLineText(altoTextLine, abbyyLine);
            addStrings(altoTextLine, abbyyLine);
            //			}
        }
    }

    private void addStrings(Element altoTextLine, Element abbyyLine) {
        List<Element> abbyyCharParams = getLineChildren(abbyyLine);

        int charCount = abbyyCharParams.size();
        String string = "";

        int l = Integer.MAX_VALUE;
        int t = Integer.MAX_VALUE;
        int r = 0;
        int b = 0;

        if (charCount == 0) {
            //create a single text element for the whole line
            Element lineFormatting = abbyyLine.getChild("formatting", abbyyNamespace);
            if (lineFormatting != null) {
                String lineText = lineFormatting.getTextTrim();
                if (lineText != null && !lineText.isEmpty()) {
                    Element altoString = new Element("String", defaultNamespace);
                    altoString.setAttribute("CONTENT", lineText);
                    altoString.setAttribute("ID", "Word_" + stringCount++);
                    altoString.setAttribute("HPOS", altoTextLine.getAttributeValue("HPOS"));
                    altoString.setAttribute("VPOS", altoTextLine.getAttributeValue("VPOS"));
                    altoString.setAttribute("HEIGHT", altoTextLine.getAttributeValue("HEIGHT"));
                    altoString.setAttribute("WIDTH", altoTextLine.getAttributeValue("WIDTH"));
                    altoTextLine.addContent(altoString);
                }
            }
        }

        for (int c = 0; c < charCount; c++) {
            if (string.isEmpty()) {
                // if first character of a new string, set hpos and vpos
                l = Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("l"));
                t = Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("t"));
            }
            if (Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("t")) < t || t == 0) {
                t = Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("t"));
            }
            if (Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("b")) > b || b == 0) {
                b = Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("b"));
            }
            string += abbyyCharParams.get(c).getValue();
            characterCount++;
            if (abbyyCharParams.get(c).getAttributeValue("charConfidence") != null && abbyyCharParams.get(c).getAttributeValue("charConfidence")
                    .length() > 0) {
                confidenceTotal += Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("charConfidence"));
            }
            try {
                if (c + 1 == charCount || abbyyCharParams.get(c + 1).getValue().equals(" ")) {
                    stringCount++;
                    r = Integer.valueOf(abbyyCharParams.get(c).getAttributeValue("r"));

                    int hpos = l;
                    int vpos = t;
                    int height = b - t;
                    int width = r - l;

                    Element altoString = new Element("String", defaultNamespace);
                    altoTextLine.addContent(altoString);
                    altoString.setAttribute("ID", "Word_" + stringCount);
                    altoString.setAttribute("CONTENT", string);
                    altoString.setAttribute("HPOS", Integer.toString(hpos));
                    altoString.setAttribute("VPOS", Integer.toString(vpos));
                    altoString.setAttribute("HEIGHT", Integer.toString(height));
                    altoString.setAttribute("WIDTH", Integer.toString(width));

                    l = Integer.MAX_VALUE;
                    t = Integer.MAX_VALUE;
                    r = 0;
                    b = 0;
                    string = "";
                    c++;

                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    private static List<Element> getChildren(Element abbyyElement) {
        List<Element> answer = new ArrayList<>();
        for (Element line : abbyyElement.getChildren("line", abbyyNamespace)) {
            List<Element> formattings = line.getChildren("formatting", abbyyNamespace);
            for (Element formatting : formattings) {
                for (Element charParams : formatting.getChildren("charParams", abbyyNamespace)) {
                    answer.add(charParams);
                }
            }
        }
        return answer;

    }

    private static List<Element> getLineChildren(Element abbyyElement) {
        List<Element> answer = new ArrayList<>();
        List<Element> formattings = abbyyElement.getChildren("formatting", abbyyNamespace);
        for (Element formatting : formattings) {
            //            List<Element> charParamsList = formatting.getChildren("charParams", abbyyNamespace);
            for (Element charParams : formatting.getChildren("charParams", abbyyNamespace)) {
                answer.add(charParams);
            }
        }
        return answer;
    }

    private static Element generatePage(String id, String height, String width, int physicalImageNr) {
        Element page = new Element("Page", defaultNamespace);
        page.setAttribute("ID", id);
        page.setAttribute("HEIGHT", height);
        page.setAttribute("WIDTH", width);
        page.setAttribute("PHYSICAL_IMG_NR", String.valueOf(physicalImageNr));
        return page;
    }

    private Element addAltoHeader() {

        Element alto = new Element("alto", defaultNamespace);
        alto.setAttribute(new Attribute("schemaLocation", "http://www.loc.gov/standards/alto/ns-v2# http://www.loc.gov/standards/alto/alto.xsd",
                xsi));

        Element description = new Element("Description", defaultNamespace);
        alto.addContent(description);

        Element measurementUnit = new Element("MeasurementUnit", defaultNamespace);
        measurementUnit.setText("pixel");
        description.addContent(measurementUnit);

        Element sourceImageInformation = new Element("sourceImageInformation", defaultNamespace);
        description.addContent(sourceImageInformation);

        Element filename = new Element("fileName", defaultNamespace);
        filename.setText(inputfilename);
        sourceImageInformation.addContent(filename);

        Element ocrProcessing = new Element("OCRProcessing", defaultNamespace);
        description.addContent(ocrProcessing);
        ocrProcessing.setAttribute("ID", "OCR_1");

        Element preProcessingStep = new Element("preProcessingStep", defaultNamespace);
        ocrProcessing.addContent(preProcessingStep);

        Element processingDateTime = new Element("processingDateTime", defaultNamespace);
        preProcessingStep.addContent(processingDateTime);
        processingDateTime.setText(DateTools.formatterISO8601DateTimeFull.print(creationtime.getTime()));

        Element processingAgency = new Element("processingAgency", defaultNamespace);
        processingAgency.setText("intranda GmbH, www.intranda.com");
        preProcessingStep.addContent(processingAgency);

        Element processingSoftware = new Element("processingSoftware", defaultNamespace);
        preProcessingStep.addContent(processingSoftware);

        Element softwareCreator = new Element("softwareCreator", defaultNamespace);
        softwareCreator.setText("intranda GmbH, Germany");
        processingSoftware.addContent(softwareCreator);

        Element softwareName = new Element("softwareName", defaultNamespace);
        softwareName.setText("intranda OCR Module for TaskManager");
        processingSoftware.addContent(softwareName);

        Element softwareVersion = new Element("softwareVersion", defaultNamespace);
        softwareVersion.setText("1.0r42");
        processingSoftware.addContent(softwareVersion);

        Element ocrProcessingStep = new Element("ocrProcessingStep", defaultNamespace);
        ocrProcessing.addContent(ocrProcessingStep);

        Element ocrProcessingSoftware = new Element("processingSoftware", defaultNamespace);
        ocrProcessingStep.addContent(ocrProcessingSoftware);

        Element ocrsoftwareCreator = new Element("softwareCreator", defaultNamespace);
        ocrsoftwareCreator.setText("ABBYY (BIT Software), Russia");
        ocrProcessingSoftware.addContent(ocrsoftwareCreator);

        Element ocrsoftwareName = new Element("softwareName", defaultNamespace);
        ocrsoftwareName.setText("FineReader Engine");
        ocrProcessingSoftware.addContent(ocrsoftwareName);

        Element ocrsoftwareVersion = new Element("softwareVersion", defaultNamespace);
        ocrsoftwareVersion.setText("10");
        ocrProcessingSoftware.addContent(ocrsoftwareVersion);

        return alto;
    }

    public static void main(String[] args) throws IOException, JDOMException {
        String inputfilename = "/home/florian/viewer/crowdsourcing/abbyydocument.xml";
        String outputfilename = "/home/florian/viewer/crowdsourcing/altodocument";
        //				if (args[0] == null) {
        //					System.exit(-1);
        //				} else {
        //					inputfilename = args[0];
        //				}
        //				if (args[1] == null) {
        //					System.exit(-1);
        //				} else {
        //					outputfilename = args[1];
        //				}
        List<File> inputList = new ArrayList<>();
        File input = new File(inputfilename);
        if (!input.exists()) {
            return;
        }
        if (input.isDirectory()) {
            inputList = Arrays.asList(input.listFiles());
        } else {
            inputList.add(input);
        }

        File outputfile = new File(outputfilename);
        if (!outputfile.exists() && !outputfile.mkdir()) {
            return;
        }

        ConvertAbbyyToAlto ab = new ConvertAbbyyToAlto();
        ab.creationtime = new Date();
        Document doc = new Document();
        for (File f : inputList) {
            doc.setRootElement(ab.convert(f));
            Format format = Format.getPrettyFormat();
            XMLOutputter xmlOut = new XMLOutputter(format);
            FileOutputStream output = new FileOutputStream(outputfile.getAbsolutePath() + File.separator + f.getName());
            xmlOut.output(doc, output);
        }
    }

    public String getInputfilename() {
        return inputfilename;
    }

    public void setInputfilename(String inputfilename) {
        this.inputfilename = inputfilename;
    }

    public Date getCreationtime() {
        return creationtime;
    }

    public void setCreationtime(Date creationtime) {
        this.creationtime = creationtime;
    }
}
