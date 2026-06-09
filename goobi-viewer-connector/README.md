# Goobi viewer - Connector

![Standard CI](https://github.com/intranda/goobi-viewer-connector/workflows/Standard%20CI/badge.svg)

> Connector application as part of the highly flexible digital library framework - made by the Goobi developer team

## Connector for the digital library framework

The Goobi viewer connector provides several connectivity methods to the indexed data content of the Goobi viewer. Currently the connector supports two ways to let other machines communicate with the Goobi viewer:

### OAI-PMH

The embedded OAI-PMH connector provides a standard OAI interface for harvesters. Dependent of the configuration it offers the following formats for receiving the content:

- METS/MODS
- Dublin Core
- MARC XML
- LIDO
- Epicur
- ESE

### SRU

The SRU connector offers the possibility to request specific information from the data store of the Goobi viewer. This is used for example by the reference manager program [Citavi](https://www.citavi.com/en/) and others to embed the Goobi viewer data store into the databases that these programs search through.

## Community

You can get in touch with the communiy in the forum. Currently the most is happening in German but please feel free to ask any questions there in English too:

https://community.goobi.io

You can find a list of Goobi viewer installations at the following URL:

https://goobi.io/viewer/installations

## Documentation

The documentation for the Goobi viewer can be found using the following URLs:

- [German](https://docs.intranda.com/goobi-viewer-de/)
- [English](https://docs.intranda.com/goobi-viewer-en/)

## Development

The development of the Goobi viewer in mostly happening by the software company [intranda GmbH](https://intranda.com). All current developments are centrally listed and explained inside of the monthy digests:

- [German](https://docs.intranda.com/goobi-viewer-digests-de/)
- [English](https://docs.intranda.com/goobi-viewer-digests-en/)

## Technical background

The Goobi viewer consists of multiple packages which all have to be installed and configured properly:

| Package                                                                                  | Function                                                                     |
| ---------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| [Goobi viewer Core](https://github.com/intranda/goobi-viewer-core)                       | Core functionality of the viewer application                                 |
| [Goobi viewer Indexer](https://github.com/intranda/goobi-viewer-indexer)                 | Indexing application to fill the Solr search index with metadata information |
| [Goobi viewer Connector](https://github.com/intranda/goobi-viewer-connector)             | Connectors for different use cases (incl. OAI-PMH, SRU)                      |
| [Goobi viewer Theme Reference](https://github.com/intranda/goobi-viewer-theme-reference) | Reference Theme for the styling of the web pages for the user interface      |

## Licence

The Goobi viewer is released under the license GPL2 or later.
Please see `LICENSE` for more information.
