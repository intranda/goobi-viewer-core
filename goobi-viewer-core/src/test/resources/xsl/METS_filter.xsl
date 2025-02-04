<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:template match="/">
        <xsl:comment>Modified via XSLT</xsl:comment>
        <xsl:apply-templates select="." mode="copy-without-filtering"/>
    </xsl:template>

    <xsl:template match="@*|node()" mode="copy-without-filtering">
        <xsl:choose>
            <xsl:when test="not(@shareable='no') and not(@USE='PRESENTATION') and not(contains(@FILEID, 'PRESENTATION'))">
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()" mode="copy-without-filtering"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>