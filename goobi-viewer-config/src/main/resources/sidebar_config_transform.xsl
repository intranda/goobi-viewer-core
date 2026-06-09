<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/">
    <xsl:copy>
      <xsl:apply-templates select="*"/>
    </xsl:copy>
  </xsl:template>

  <!-- Main sidebar transformation -->
  <xsl:template match="sidebar">
    <sidebar>
        <!-- Hardcoded views block -->
        <views>
            <view name="calendar">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="search-in-current-item" />
                <displayWidget name="named-entities" />
                <displayWidget name="metadata" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="downloads" />
                <displayWidget name="citation" />
                <displayWidget name="related-groups" />
                <displayWidget name="toc" />
                <displayWidget name="version-history" />
                <displayWidget name="user-interactions" />
            </view>
            <view name="fulltext">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="search-in-current-item" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="downloads" />
                <displayWidget name="version-history" />
            </view>
            <view name="metadata">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="search-in-current-item" />
                <displayWidget name="named-entities" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="downloads" />
                <displayWidget name="formats-links" />
                <displayWidget name="citation" />
                <displayWidget name="version-history" />
            </view>
            <view name="named-entities">
                <displayWidget name="views" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="version-history" />
            </view>
            <view name="object">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="search-in-current-item" />
                <displayWidget name="named-entities" />
                <displayWidget name="metadata" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="downloads" />
                <displayWidget name="citation" />
                <displayWidget name="related-groups" />
                <displayWidget name="toc" />
                <displayWidget name="version-history" />
            </view>
            <view name="thumbs">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="version-history" />
            </view>
            <view name="toc">
                <displayWidget name="views" />
                <displayWidget name="copyright-info" />
                <displayWidget name="search-hit-navigation" />
                <displayWidget name="geomap" />
                <displayWidget name="annotations" />
                <displayWidget name="record-notes" />
                <displayWidget name="formats-links" />
                <displayWidget name="version-history" />
            </view>
        </views>

      <!-- Begin widgets section -->
      <widgets>
        <!-- widget[@name="views"] -->
        <widget name="views">
          <calendar enabled="true"/>
          <xsl:copy-of select="fulltext"/>
          <xsl:copy-of select="metadata"/>
          <xsl:if test="opac">
            <opac>
              <xsl:copy-of select="opac/@* | opac/*"/>
            </opac>
          </xsl:if>
          <xsl:if test="page">
            <object>
              <xsl:copy-of select="page/@*"/>
            </object>
          </xsl:if>
          <xsl:copy-of select="toc"/>
          <xsl:copy-of select="thumbs"/>
        </widget>

        <!-- widget[@name="archives"] (empty) -->
        <widget name="archives"/>

        <!-- widget[@name="copyright-info"] -->
        <xsl:if test="copyrightIndicator">
          <widget name="copyright-info">
            <xsl:copy-of select="copyrightIndicator/*"/>
          </widget>
        </xsl:if>

        <!-- widget[@name="downloads"] from copyrightIndicator -->
        <xsl:if test="copyrightIndicator">
          <widget name="downloads">
            <xsl:copy-of select="copyrightIndicator/*"/>
          </widget>
        </xsl:if>

        <!-- widget[@name="browsing-terms"] -->
        <xsl:if test="sidebarBrowsingTerms">
          <widget name="browsing-terms">
            <xsl:copy-of select="sidebarBrowsingTerms/@*"/>
            <xsl:copy-of select="sidebarBrowsingTerms/*"/>
          </widget>
        </xsl:if>

        <!-- widget[@name="rss"] -->
        <xsl:if test="sidebarRssFeed">
          <widget name="rss">
            <xsl:copy-of select="sidebarRssFeed/@*"/>
            <xsl:copy-of select="sidebarRssFeed/*"/>
          </widget>
        </xsl:if>

        <!-- widget[@name="toc"] -->
        <xsl:if test="sidebarToc">
          <widget name="toc">
            <xsl:copy-of select="sidebarToc/*"/>
          </widget>
        </xsl:if>

        <!-- widget[@name="additional-files"] -->
        <xsl:if test="sidebarWidgetAdditionalFiles">
          <widget name="additional-files">
            <xsl:copy-of select="sidebarWidgetAdditionalFiles/*"/>
          </widget>
        </xsl:if>

        <!-- Handle sidebarWidgetUsage split -->
        <xsl:if test="sidebarWidgetUsage">
          <!-- widget[@name="citation"] -->
          <widget name="citation">
            <xsl:for-each select="sidebarWidgetUsage/*[contains(name(), 'citation')]">
              <xsl:copy-of select="."/>
            </xsl:for-each>
          </widget>
          <!-- widget[@name="downloads"] -->
          <widget name="downloads">
            <xsl:for-each select="sidebarWidgetUsage/*[not(contains(name(), 'citation'))]">
              <xsl:copy-of select="."/>
            </xsl:for-each>
          </widget>
        </xsl:if>
      </widgets>
    </sidebar>
  </xsl:template>

  <!-- Copy everything else unchanged -->
  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="@* | *"/>
    </xsl:copy>
  </xsl:template>

  <!-- Copy attributes -->
  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>
</xsl:stylesheet>
