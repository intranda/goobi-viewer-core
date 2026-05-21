<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSLT stylesheet that converts Solr response XML into RIS (Research Information Systems) format.

  Field mappings mirror MetadataTools.generateRIS() so that XSLT-based export produces
  the same output as the Java implementation.

  Input format (Solr):
    <result name="response" numFound="N" start="0">
      <doc>
        <str name="DOCSTRCT">monograph</str>
        <str name="MD_TITLE">Some Title</str>
        ...
      </doc>
    </result>

  Output: plain-text RIS entries separated by "ER  -".
-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" encoding="UTF-8" />

    <xsl:template match="/result">
        <xsl:apply-templates select="doc" />
    </xsl:template>

    <xsl:template match="doc">
        <!-- TY - Type of reference -->
        <xsl:text>TY  - </xsl:text>
        <xsl:call-template name="mapDocStructType">
            <xsl:with-param name="type" select="str[@name='DOCSTRCT'] | str[@name='DOCSTRCT_TOP']" />
        </xsl:call-template>
        <xsl:text>&#13;&#10;</xsl:text>

        <!-- TI - Title -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'TI'" />
            <xsl:with-param name="nodes" select="str[@name='MD_TITLE'] | arr[@name='MD_TITLE']/str" />
        </xsl:call-template>

        <!-- AU - Authors (each value on its own line) -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'AU'" />
            <xsl:with-param name="nodes" select="str[@name='MD_AUTHOR'] | arr[@name='MD_AUTHOR']/str" />
        </xsl:call-template>

        <!-- ED - Editors -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'ED'" />
            <xsl:with-param name="nodes" select="str[@name='MD_EDITOR'] | arr[@name='MD_EDITOR']/str" />
        </xsl:call-template>

        <!-- J2 - Alternate title -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'J2'" />
            <xsl:with-param name="nodes" select="str[@name='MD_ALTERNATETITLE'] | arr[@name='MD_ALTERNATETITLE']/str" />
        </xsl:call-template>

        <!-- PY - Year -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'PY'" />
            <xsl:with-param name="nodes" select="str[@name='MD_YEARPUBLISH'] | arr[@name='MD_YEARPUBLISH']/str" />
        </xsl:call-template>

        <!-- PB - Publisher -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'PB'" />
            <xsl:with-param name="nodes" select="str[@name='MD_PUBLISHER'] | arr[@name='MD_PUBLISHER']/str" />
        </xsl:call-template>

        <!-- CY - Place of publication -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'CY'" />
            <xsl:with-param name="nodes" select="str[@name='MD_PLACEPUBLISH'] | arr[@name='MD_PLACEPUBLISH']/str" />
        </xsl:call-template>

        <!-- SN - ISBN / ISSN -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'SN'" />
            <xsl:with-param name="nodes" select="str[@name='MD_ISBN'] | arr[@name='MD_ISBN']/str | str[@name='MD_ISSN'] | arr[@name='MD_ISSN']/str" />
        </xsl:call-template>

        <!-- AB - Abstract / Information -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'AB'" />
            <xsl:with-param name="nodes" select="str[@name='MD_ABSTRACT'] | arr[@name='MD_ABSTRACT']/str | str[@name='MD_INFORMATION'] | arr[@name='MD_INFORMATION']/str" />
        </xsl:call-template>

        <!-- KW - Keywords -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'KW'" />
            <xsl:with-param name="nodes" select="str[@name='MD_GEOKEYWORD'] | arr[@name='MD_GEOKEYWORD']/str | str[@name='MD_PERSONKEYWORD'] | arr[@name='MD_PERSONKEYWORD']/str | str[@name='MD_WORKKEYWORD'] | arr[@name='MD_WORKKEYWORD']/str" />
        </xsl:call-template>

        <!-- LA - Language -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'LA'" />
            <xsl:with-param name="nodes" select="str[@name='MD_LANGUAGE'] | arr[@name='MD_LANGUAGE']/str" />
        </xsl:call-template>

        <!-- N1 - Notes -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'N1'" />
            <xsl:with-param name="nodes" select="str[@name='MD_NOTE'] | arr[@name='MD_NOTE']/str" />
        </xsl:call-template>

        <!-- ET - Edition -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'ET'" />
            <xsl:with-param name="nodes" select="str[@name='MD_EDITION'] | arr[@name='MD_EDITION']/str" />
        </xsl:call-template>

        <!-- VL - Volume -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'VL'" />
            <xsl:with-param name="nodes" select="str[@name='CURRENTNO'] | arr[@name='CURRENTNO']/str" />
        </xsl:call-template>

        <!-- SP - Number of pages -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'SP'" />
            <xsl:with-param name="nodes" select="str[@name='NUMPAGES'] | arr[@name='NUMPAGES']/str" />
        </xsl:call-template>

        <!-- NV - Number of volumes -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'NV'" />
            <xsl:with-param name="nodes" select="str[@name='NUMVOLUMES'] | arr[@name='NUMVOLUMES']/str" />
        </xsl:call-template>

        <!-- CN - Call number / PI -->
        <xsl:call-template name="risField">
            <xsl:with-param name="tag" select="'CN'" />
            <xsl:with-param name="nodes" select="str[@name='PI_TOPSTRUCT'] | str[@name='PI']" />
        </xsl:call-template>

        <!-- ER - End of reference -->
        <xsl:text>ER  - &#13;&#10;</xsl:text>
    </xsl:template>

    <!-- Emits one RIS line per node: "TAG  - value\r\n" -->
    <xsl:template name="risField">
        <xsl:param name="tag" />
        <xsl:param name="nodes" />
        <xsl:for-each select="$nodes">
            <xsl:value-of select="$tag" />
            <xsl:text>  - </xsl:text>
            <xsl:value-of select="." />
            <xsl:text>&#13;&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>

    <!-- Maps Goobi docstruct types to RIS reference types (mirrors MetadataTools.getRISTypeMapping) -->
    <xsl:template name="mapDocStructType">
        <xsl:param name="type" />
        <xsl:variable name="lower" select="translate($type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')" />
        <xsl:choose>
            <xsl:when test="$lower = 'abstract'">ABST</xsl:when>
            <xsl:when test="$lower = 'article'">MGZN</xsl:when>
            <xsl:when test="$lower = 'audio'">AUDIO</xsl:when>
            <xsl:when test="$lower = 'chapter'">CHAP</xsl:when>
            <xsl:when test="$lower = 'figure' or $lower = 'picture'">FIGURE</xsl:when>
            <xsl:when test="$lower = 'manuscript'">MANSCPT</xsl:when>
            <xsl:when test="$lower = 'monograph'">BOOK</xsl:when>
            <xsl:when test="$lower = 'map'">MAP</xsl:when>
            <xsl:when test="$lower = 'mutivolumework' or $lower = 'multivolume_work'">SER</xsl:when>
            <xsl:when test="$lower = 'periodical'">JFULL</xsl:when>
            <xsl:when test="$lower = 'periodicalvolume' or $lower = 'periodical_volume'">JOUR</xsl:when>
            <xsl:when test="$lower = 'sheetmusic'">MUSIC</xsl:when>
            <xsl:when test="$lower = 'video'">VIDEO</xsl:when>
            <xsl:otherwise>GEN</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
