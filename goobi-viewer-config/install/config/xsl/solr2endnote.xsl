<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSLT stylesheet that converts Solr response XML into Endnote XML format.

  Input format (Solr):
    <result name="response" numFound="N" start="0">
      <doc>
        <str name="DOCSTRCT">monograph</str>
        <str name="MD_TITLE">Some Title</str>
        ...
      </doc>
    </result>

  Output format (Endnote XML):
    <xml><records><record>...</record></records></xml>
-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" encoding="UTF-8" />

    <xsl:template match="/result">
        <xml>
            <records>
                <xsl:apply-templates select="doc" />
            </records>
        </xml>
    </xsl:template>

    <xsl:template match="doc">
        <record>
            <!-- Reference type -->
            <ref-type>
                <xsl:attribute name="name">
                    <xsl:call-template name="mapDocStructType">
                        <xsl:with-param name="type" select="str[@name='DOCSTRCT'] | str[@name='DOCSTRCT_TOP']" />
                    </xsl:call-template>
                </xsl:attribute>
            </ref-type>

            <!-- Contributors / authors -->
            <xsl:if test="str[@name='MD_AUTHOR'] or arr[@name='MD_AUTHOR']">
                <contributors>
                    <authors>
                        <xsl:for-each select="str[@name='MD_AUTHOR'] | arr[@name='MD_AUTHOR']/str">
                            <author><xsl:value-of select="." /></author>
                        </xsl:for-each>
                    </authors>
                </contributors>
            </xsl:if>

            <!-- Editors -->
            <xsl:if test="str[@name='MD_EDITOR'] or arr[@name='MD_EDITOR']">
                <contributors>
                    <secondary-authors>
                        <xsl:for-each select="str[@name='MD_EDITOR'] | arr[@name='MD_EDITOR']/str">
                            <author><xsl:value-of select="." /></author>
                        </xsl:for-each>
                    </secondary-authors>
                </contributors>
            </xsl:if>

            <!-- Title -->
            <xsl:if test="str[@name='MD_TITLE'] or arr[@name='MD_TITLE']">
                <titles>
                    <title>
                        <xsl:value-of select="str[@name='MD_TITLE'] | arr[@name='MD_TITLE']/str[1]" />
                    </title>
                    <xsl:if test="str[@name='MD_ALTERNATETITLE'] or arr[@name='MD_ALTERNATETITLE']">
                        <secondary-title>
                            <xsl:value-of select="str[@name='MD_ALTERNATETITLE'] | arr[@name='MD_ALTERNATETITLE']/str[1]" />
                        </secondary-title>
                    </xsl:if>
                </titles>
            </xsl:if>

            <!-- Year -->
            <xsl:if test="str[@name='MD_YEARPUBLISH'] or arr[@name='MD_YEARPUBLISH']">
                <dates>
                    <year>
                        <xsl:value-of select="str[@name='MD_YEARPUBLISH'] | arr[@name='MD_YEARPUBLISH']/str[1]" />
                    </year>
                </dates>
            </xsl:if>

            <!-- Publisher -->
            <xsl:if test="str[@name='MD_PUBLISHER'] or arr[@name='MD_PUBLISHER']">
                <publisher>
                    <xsl:value-of select="str[@name='MD_PUBLISHER'] | arr[@name='MD_PUBLISHER']/str[1]" />
                </publisher>
            </xsl:if>

            <!-- Place of publication -->
            <xsl:if test="str[@name='MD_PLACEPUBLISH'] or arr[@name='MD_PLACEPUBLISH']">
                <pub-location>
                    <xsl:value-of select="str[@name='MD_PLACEPUBLISH'] | arr[@name='MD_PLACEPUBLISH']/str[1]" />
                </pub-location>
            </xsl:if>

            <!-- ISBN / ISSN -->
            <xsl:if test="str[@name='MD_ISBN'] or arr[@name='MD_ISBN'] or str[@name='MD_ISSN'] or arr[@name='MD_ISSN']">
                <isbn>
                    <xsl:value-of select="str[@name='MD_ISBN'] | arr[@name='MD_ISBN']/str[1] | str[@name='MD_ISSN'] | arr[@name='MD_ISSN']/str[1]" />
                </isbn>
            </xsl:if>

            <!-- Abstract -->
            <xsl:if test="str[@name='MD_ABSTRACT'] or arr[@name='MD_ABSTRACT'] or str[@name='MD_INFORMATION'] or arr[@name='MD_INFORMATION']">
                <abstract>
                    <xsl:value-of select="str[@name='MD_ABSTRACT'] | arr[@name='MD_ABSTRACT']/str[1] | str[@name='MD_INFORMATION'] | arr[@name='MD_INFORMATION']/str[1]" />
                </abstract>
            </xsl:if>

            <!-- Keywords -->
            <xsl:if test="str[@name='MD_GEOKEYWORD'] or arr[@name='MD_GEOKEYWORD'] or str[@name='MD_PERSONKEYWORD'] or arr[@name='MD_PERSONKEYWORD'] or str[@name='MD_WORKKEYWORD'] or arr[@name='MD_WORKKEYWORD']">
                <keywords>
                    <xsl:for-each select="str[@name='MD_GEOKEYWORD'] | arr[@name='MD_GEOKEYWORD']/str | str[@name='MD_PERSONKEYWORD'] | arr[@name='MD_PERSONKEYWORD']/str | str[@name='MD_WORKKEYWORD'] | arr[@name='MD_WORKKEYWORD']/str">
                        <keyword><xsl:value-of select="." /></keyword>
                    </xsl:for-each>
                </keywords>
            </xsl:if>

            <!-- Language -->
            <xsl:if test="str[@name='MD_LANGUAGE'] or arr[@name='MD_LANGUAGE']">
                <language>
                    <xsl:value-of select="str[@name='MD_LANGUAGE'] | arr[@name='MD_LANGUAGE']/str[1]" />
                </language>
            </xsl:if>

            <!-- Volume -->
            <xsl:if test="str[@name='CURRENTNO'] or arr[@name='CURRENTNO']">
                <volume>
                    <xsl:value-of select="str[@name='CURRENTNO'] | arr[@name='CURRENTNO']/str[1]" />
                </volume>
            </xsl:if>

            <!-- Edition -->
            <xsl:if test="str[@name='MD_EDITION'] or arr[@name='MD_EDITION']">
                <edition>
                    <xsl:value-of select="str[@name='MD_EDITION'] | arr[@name='MD_EDITION']/str[1]" />
                </edition>
            </xsl:if>

            <!-- Number of pages -->
            <xsl:if test="str[@name='NUMPAGES']">
                <pages>
                    <xsl:value-of select="str[@name='NUMPAGES']" />
                </pages>
            </xsl:if>

            <!-- Notes -->
            <xsl:if test="str[@name='MD_NOTE'] or arr[@name='MD_NOTE']">
                <notes>
                    <xsl:value-of select="str[@name='MD_NOTE'] | arr[@name='MD_NOTE']/str[1]" />
                </notes>
            </xsl:if>

            <!-- PI as accession number -->
            <xsl:if test="str[@name='PI'] or str[@name='PI_TOPSTRUCT']">
                <accession-num>
                    <xsl:value-of select="str[@name='PI'] | str[@name='PI_TOPSTRUCT']" />
                </accession-num>
            </xsl:if>
        </record>
    </xsl:template>

    <!-- Maps Goobi docstruct types to Endnote reference type names -->
    <xsl:template name="mapDocStructType">
        <xsl:param name="type" />
        <xsl:variable name="lower" select="translate($type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')" />
        <xsl:choose>
            <xsl:when test="$lower = 'monograph'">Book</xsl:when>
            <xsl:when test="$lower = 'article'">Journal Article</xsl:when>
            <xsl:when test="$lower = 'periodical'">Journal Article</xsl:when>
            <xsl:when test="$lower = 'manuscript'">Manuscript</xsl:when>
            <xsl:when test="$lower = 'chapter'">Book Section</xsl:when>
            <xsl:when test="$lower = 'volume'">Book</xsl:when>
            <xsl:when test="$lower = 'periodicalvolume' or $lower = 'periodical_volume'">Journal Article</xsl:when>
            <xsl:when test="$lower = 'proceedings'">Conference Proceedings</xsl:when>
            <xsl:when test="$lower = 'thesis'">Thesis</xsl:when>
            <xsl:when test="$lower = 'map'">Map</xsl:when>
            <xsl:otherwise>Generic</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
