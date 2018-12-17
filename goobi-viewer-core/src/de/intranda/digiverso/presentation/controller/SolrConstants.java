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

import java.util.Locale;

/**
 * This class provides constants for Lucene in alphabetical order.
 */
public class SolrConstants {

    public enum DocType {
        ACCESSDENIED,
        DOCSTRCT,
        PAGE,
        METADATA, // grouped metadata
        EVENT, // LIDO event
        UGC, // user-generated content
        GROUP; // convolute

        public static DocType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "ACCESSDENIED":
                        return ACCESSDENIED;
                    case "DOCSTRCT":
                        return DOCSTRCT;
                    case "PAGE":
                        return PAGE;
                    case "METADATA":
                        return METADATA;
                    case "EVENT":
                        return EVENT;
                    case "UGC":
                        return UGC;
                    case "GROUP":
                        return GROUP;
                    default:
                        return null;
                }
            }

            return null;
        }

        public String getLabel(Locale locale) {
            return Helper.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
        }
    }

    public enum MetadataGroupType {
        PERSON,
        CORPORATION,
        LOCATION,
        SUBJECT,
        ORIGININFO,
        RECORD,
        OTHER;

        public static MetadataGroupType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "PERSON":
                        return PERSON;
                    case "CORPORATION":
                        return CORPORATION;
                    case "LOCATION":
                        return LOCATION;
                    case "SUBJECT":
                        return SUBJECT;
                    case "ORIGININFO":
                        return ORIGININFO;
                    case "OTHER":
                        return OTHER;
                    default:
                        return null;
                }

            }

            return null;
        }
    }

    public static final String ACCESSCONDITION = "ACCESSCONDITION";
    //    public static final String ALTO = "ALTO";
    public static final String CURRENTNO = "CURRENTNO";
    public static final String CURRENTNOSORT = "CURRENTNOSORT";
    public static final String DATAREPOSITORY = "DATAREPOSITORY";
    public static final String DATECREATED = "DATECREATED";
    public static final String DATEDELETED = "DATEDELETED";
    public static final String DATEUPDATED = "DATEUPDATED";
    public static final String DEFAULT = "DEFAULT";
    public static final String DC = "DC";
    public static final String DOCSTRCT = "DOCSTRCT";
    public static final String DOCSTRCT_SUB = "DOCSTRCT_SUB";
    public static final String DOCSTRCT_TOP = "DOCSTRCT_TOP";
    public static final String DOCTYPE = "DOCTYPE";
    public static final String EVENTDATE = "EVENTDATE";
    public static final String EVENTDATESTART = "EVENTDATESTART";
    public static final String EVENTDATEEND = "EVENTDATEEND";
    public static final String EVENTTYPE = "EVENTTYPE";
    public static final String IDDOC = "IDDOC";
    public static final String IDDOC_OWNER = "IDDOC_OWNER";
    public static final String IDDOC_PARENT = "IDDOC_PARENT";
    public static final String IDDOC_TOPSTRUCT = "IDDOC_TOPSTRUCT";
    public static final String FILEIDROOT = "FILEIDROOT";
    public static final String FILENAME = "FILENAME";
    public static final String FILENAME_ALTO = "FILENAME_ALTO";
    public static final String FILENAME_FULLTEXT = "FILENAME_FULLTEXT";
    public static final String FILENAME_HTML_SANDBOXED = "FILENAME_HTML-SANDBOXED";
    public static final String FILENAME_MPEG = "FILENAME_MPEG";
    public static final String FILENAME_MPEG3 = "FILENAME_MPEG3";
    public static final String FILENAME_MP4 = "FILENAME_MP4";
    public static final String FILENAME_OGG = "FILENAME_OGG";
    public static final String FILENAME_TEI = "FILENAME_TEI";
    public static final String FILENAME_WEBM = "FILENAME_WEBM";
    public static final String FULLTEXT = "FULLTEXT";
    public static final String FULLTEXTAVAILABLE = "FULLTEXTAVAILABLE";
    public static final String GROUPFIELD = "GROUPFIELD";
    public static final String GROUPTYPE = "GROUPTYPE";
    public static final String HEIGHT = "HEIGHT";
    public static final String IMAGEURN = "IMAGEURN";
    public static final String IMAGEURN_OAI = "IMAGEURN_OAI";
    public static final String ISANCHOR = "ISANCHOR";
    public static final String ISWORK = "ISWORK";
    public static final String LABEL = "LABEL";
    public static final String LANGUAGE = "LANGUAGE";
    public static final String LOGID = "LOGID";
    public static final String METADATATYPE = "METADATATYPE";
    public static final String NORMDATATERMS = "NORMDATATERMS";
    public static final String MIMETYPE = "MIMETYPE";
    public static final String NUMPAGES = "NUMPAGES";
    public static final String NUMVOLUMES = "NUMVOLUMES";
    public static final String OPACURL = "OPACURL";
    public static final String ORDER = "ORDER";
    public static final String ORDERLABEL = "ORDERLABEL";
    public static final String OVERVIEWPAGE = "OVERVIEWPAGE";
    public static final String OVERVIEWPAGE_DESCRIPTION = "OVERVIEWPAGE_DESCRIPTION";
    public static final String OVERVIEWPAGE_PUBLICATIONTEXT = "OVERVIEWPAGE_PUBLICATIONTEXT";
    public static final String PERSON_ONEFIELD = "MD_CREATOR";
    public static final String PHYSID = "PHYSID";
    public static final String PI = "PI";
    public static final String PI_ANCHOR = "PI_ANCHOR";
    public static final String PI_PARENT = "PI_PARENT";
    public static final String PI_TOPSTRUCT = "PI_TOPSTRUCT";
    public static final String PLACEPUBLISH = "MD_PLACEPUBLISH";
    public static final String PUBLISHER = "PUBLISHER";
    public static final String RESOURCE = "RESOURCE";
    public static final String SOURCEDOCFORMAT = "SOURCEDOCFORMAT";
    public static final String SUBTITLE = "SUBTITLE";
    public static final String SUPERDEFAULT = "SUPERDEFAULT";
    public static final String SUPERFULLTEXT = "SUPERFULLTEXT";
    public static final String TITLE = "MD_TITLE";
    public static final String THUMBNAIL = "THUMBNAIL";
    public static final String THUMBPAGENO = "THUMBPAGENO";
    public static final String THUMBPAGENOLABEL = "THUMBPAGENOLABEL";
    public static final String UGCCOORDS = "UGCCOORDS";
    public static final String UGCTERMS = "UGCTERMS";
    public static final String UGCTYPE = "UGCTYPE";
    public static final String URN = "URN";
    public static final String WIDTH = "WIDTH";
    public static final String YEARPUBLISH = "MD_YEARPUBLISH";

    public static final String OPEN_ACCESS_VALUE = "OPENACCESS";

    public static final String GROUPID_ = "GROUPID_";
    public static final String GROUPORDER_ = "GROUPORDER_";
    public static final String _LANG_ = "_LANG_";
    public static final String _NOESCAPE = "_NOESCAPE";
    public static final String _UNTOKENIZED = "_UNTOKENIZED";
    public static final String _DRILLDOWN_SUFFIX = "_DD";
    public static final String _METS = "METS";
    public static final String _LIDO = "LIDO";

    public static final String _CALENDAR_YEAR = "YEAR";
    public static final String _CALENDAR_MONTH = "YEARMONTH";
    public static final String _CALENDAR_DAY = "YEARMONTHDAY";

    public static final String MDNUM_FILESIZE = "MDNUM_FILESIZE";

    public static final String FACET_DC = "FACET_DC";
}
