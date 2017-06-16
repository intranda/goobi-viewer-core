Bei der Installation nun beachten:
--------------------------------------

- alle Configs werden aus dem src-Ordner übernommen
- alle C:/digiverso/ werden im *.war ersetzt durch /opt/digiverso/viewer/
- zum Kompilieren der Ant-Tasks muss man in den Run-Configurations von Ant die servlet-api des Tomcat zum Classpath hinzufügen
- die Datei MODS2MARC21slim.xsl muss im Ordner /opt/digiverso/viewer/oai/ liegen
- config_imageFooter.xml muss liegen unter /opt/digiverso/viewer/config/
- config_pdfTitlePage.xml muss liegen unter /opt/digiverso/viewer/config/