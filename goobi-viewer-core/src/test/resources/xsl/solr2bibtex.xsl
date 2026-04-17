<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSLT stylesheet that converts Solr response XML into BibTeX format.

  Input format (Solr):
    <result name="response" numFound="N" start="0">
      <doc>
        <str name="DOCSTRCT">monograph</str>
        <str name="MD_TITLE">Some Title</str>
        ...
      </doc>
    </result>

  Output: plain-text BibTeX entries.
-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" encoding="UTF-8" />

    <xsl:template match="/result">
        <xsl:apply-templates select="doc" />
    </xsl:template>

    <xsl:template match="doc">
        <!-- Determine the BibTeX entry type -->
        <xsl:variable name="entryType">
            <xsl:call-template name="mapDocStructType">
                <xsl:with-param name="type" select="str[@name='DOCSTRCT'] | str[@name='DOCSTRCT_TOP']" />
            </xsl:call-template>
        </xsl:variable>

        <!-- Determine citation key from PI or position -->
        <xsl:variable name="citeKey">
            <xsl:choose>
                <xsl:when test="str[@name='PI']"><xsl:value-of select="str[@name='PI']" /></xsl:when>
                <xsl:when test="str[@name='PI_TOPSTRUCT']"><xsl:value-of select="str[@name='PI_TOPSTRUCT']" /></xsl:when>
                <xsl:otherwise>entry<xsl:value-of select="position()" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:text>@</xsl:text>
        <xsl:value-of select="$entryType" />
        <xsl:text>{</xsl:text>
        <xsl:value-of select="$citeKey" />
        <xsl:text>,&#10;</xsl:text>

        <!-- Author -->
        <xsl:if test="str[@name='MD_AUTHOR'] or arr[@name='MD_AUTHOR']">
            <xsl:text>  author = {</xsl:text>
            <xsl:for-each select="str[@name='MD_AUTHOR'] | arr[@name='MD_AUTHOR']/str">
                <xsl:if test="position() &gt; 1"> and </xsl:if>
                <xsl:value-of select="." />
            </xsl:for-each>
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Editor -->
        <xsl:if test="str[@name='MD_EDITOR'] or arr[@name='MD_EDITOR']">
            <xsl:text>  editor = {</xsl:text>
            <xsl:for-each select="str[@name='MD_EDITOR'] | arr[@name='MD_EDITOR']/str">
                <xsl:if test="position() &gt; 1"> and </xsl:if>
                <xsl:value-of select="." />
            </xsl:for-each>
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Title -->
        <xsl:if test="str[@name='MD_TITLE'] or arr[@name='MD_TITLE']">
            <xsl:text>  title = {</xsl:text>
            <xsl:value-of select="str[@name='MD_TITLE'] | arr[@name='MD_TITLE']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Year -->
        <xsl:if test="str[@name='MD_YEARPUBLISH'] or arr[@name='MD_YEARPUBLISH']">
            <xsl:text>  year = {</xsl:text>
            <xsl:value-of select="str[@name='MD_YEARPUBLISH'] | arr[@name='MD_YEARPUBLISH']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Publisher -->
        <xsl:if test="str[@name='MD_PUBLISHER'] or arr[@name='MD_PUBLISHER']">
            <xsl:text>  publisher = {</xsl:text>
            <xsl:value-of select="str[@name='MD_PUBLISHER'] | arr[@name='MD_PUBLISHER']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Address / place of publication -->
        <xsl:if test="str[@name='MD_PLACEPUBLISH'] or arr[@name='MD_PLACEPUBLISH']">
            <xsl:text>  address = {</xsl:text>
            <xsl:value-of select="str[@name='MD_PLACEPUBLISH'] | arr[@name='MD_PLACEPUBLISH']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- ISBN -->
        <xsl:if test="str[@name='MD_ISBN'] or arr[@name='MD_ISBN']">
            <xsl:text>  isbn = {</xsl:text>
            <xsl:value-of select="str[@name='MD_ISBN'] | arr[@name='MD_ISBN']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- ISSN -->
        <xsl:if test="str[@name='MD_ISSN'] or arr[@name='MD_ISSN']">
            <xsl:text>  issn = {</xsl:text>
            <xsl:value-of select="str[@name='MD_ISSN'] | arr[@name='MD_ISSN']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Abstract -->
        <xsl:if test="str[@name='MD_ABSTRACT'] or arr[@name='MD_ABSTRACT']">
            <xsl:text>  abstract = {</xsl:text>
            <xsl:value-of select="str[@name='MD_ABSTRACT'] | arr[@name='MD_ABSTRACT']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Keywords -->
        <xsl:if test="str[@name='MD_GEOKEYWORD'] or arr[@name='MD_GEOKEYWORD'] or str[@name='MD_PERSONKEYWORD'] or arr[@name='MD_PERSONKEYWORD'] or str[@name='MD_WORKKEYWORD'] or arr[@name='MD_WORKKEYWORD']">
            <xsl:text>  keywords = {</xsl:text>
            <xsl:for-each select="str[@name='MD_GEOKEYWORD'] | arr[@name='MD_GEOKEYWORD']/str | str[@name='MD_PERSONKEYWORD'] | arr[@name='MD_PERSONKEYWORD']/str | str[@name='MD_WORKKEYWORD'] | arr[@name='MD_WORKKEYWORD']/str">
                <xsl:if test="position() &gt; 1">, </xsl:if>
                <xsl:value-of select="." />
            </xsl:for-each>
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Language -->
        <xsl:if test="str[@name='MD_LANGUAGE'] or arr[@name='MD_LANGUAGE']">
            <xsl:text>  language = {</xsl:text>
            <xsl:value-of select="str[@name='MD_LANGUAGE'] | arr[@name='MD_LANGUAGE']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Volume -->
        <xsl:if test="str[@name='CURRENTNO'] or arr[@name='CURRENTNO']">
            <xsl:text>  volume = {</xsl:text>
            <xsl:value-of select="str[@name='CURRENTNO'] | arr[@name='CURRENTNO']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Edition -->
        <xsl:if test="str[@name='MD_EDITION'] or arr[@name='MD_EDITION']">
            <xsl:text>  edition = {</xsl:text>
            <xsl:value-of select="str[@name='MD_EDITION'] | arr[@name='MD_EDITION']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Pages -->
        <xsl:if test="str[@name='NUMPAGES']">
            <xsl:text>  pages = {</xsl:text>
            <xsl:value-of select="str[@name='NUMPAGES']" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <!-- Note -->
        <xsl:if test="str[@name='MD_NOTE'] or arr[@name='MD_NOTE']">
            <xsl:text>  note = {</xsl:text>
            <xsl:value-of select="str[@name='MD_NOTE'] | arr[@name='MD_NOTE']/str[1]" />
            <xsl:text>},&#10;</xsl:text>
        </xsl:if>

        <xsl:text>}&#10;&#10;</xsl:text>
    </xsl:template>

    <!-- Maps Goobi docstruct types to BibTeX entry types -->
    <xsl:template name="mapDocStructType">
        <xsl:param name="type" />
        <xsl:variable name="lower" select="translate($type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')" />
        <xsl:choose>
            <xsl:when test="$lower = 'monograph'">book</xsl:when>
            <xsl:when test="$lower = 'article'">article</xsl:when>
            <xsl:when test="$lower = 'periodical'">article</xsl:when>
            <xsl:when test="$lower = 'manuscript'">unpublished</xsl:when>
            <xsl:when test="$lower = 'chapter'">incollection</xsl:when>
            <xsl:when test="$lower = 'volume'">book</xsl:when>
            <xsl:when test="$lower = 'periodicalvolume' or $lower = 'periodical_volume'">article</xsl:when>
            <xsl:when test="$lower = 'proceedings'">proceedings</xsl:when>
            <xsl:when test="$lower = 'thesis'">phdthesis</xsl:when>
            <xsl:otherwise>misc</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
