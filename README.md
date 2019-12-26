# Goobi viewer - Core
[![Build Status](https://travis-ci.org/intranda/goobi-viewer-core.svg?branch=master)](https://travis-ci.org/intranda/goobi-viewer-core)
> A highly flexible digital library framework - made by the Goobi developer team


## What is the Goobi viewer?
The Goobi viewer is a web application that allows digitised material to be displayed in a web browser. It is used as central basis for an online digital library and offers various functionalities as browsing through digital collections, complex full-text searches, displaying images based on the IIIF standard, deliver audio and video material and a lot more. You can find more information at <https://goobi.io/viewer>

![alt Goobi viewer - image view of a book page](doc/images/goobi_viewer_theme_reference_image_display.png "Goobi viewer - image view of a book page")


## Community
You can get in touch with the communiy in the forum. Currently the most is happening in German but please feel free to ask any questions there in English too:

https://community.goobi.io

You can find a list of Goobi viewer installations at the following URL:

https://goobi.io/viewer/installations


## Documentation
The documentation for the Goobi viewer can be found using the following URLs:

* [German](https://docs.intranda.com/goobi-viewer-de/)
* [English](https://docs.intranda.com/goobi-viewer-en/)


## Development
The development of the Goobi viewer in mostly happening by the software company [intranda GmbH](https://intranda.com). All current developments are centrally listed and explained inside of the monthy digests:

* [German](https://docs.intranda.com/goobi-viewer-digests-de/)
* [English](https://docs.intranda.com/goobi-viewer-digests-en/)


## Technical background
The Goobi viewer consists of multiple packages which all have to be installed and configured properly:

| Package                                                                                  | Function                                                                     |
| ------                                                                                   | ------                                                                       |
| [Goobi viewer Core](https://github.com/intranda/goobi-viewer-core)                       | Core functionality of the viewer application                                 |
| [Goobi viewer Indexer](https://github.com/intranda/goobi-viewer-indexer)                 | Indexing application to fill the Solr search index with metadata information |
| [Goobi viewer Connector](https://github.com/intranda/goobi-viewer-connector)             | Connectors for different use cases (incl. OAI-PMH, SRU)                      |
| [Goobi viewer Theme Reference](https://github.com/intranda/goobi-viewer-theme-reference) | Reference Theme for the styling of the web pages for the user interface      |


## Licence
The Goobi viewer is released under the license GPL2 or later.
Please see ``LICENSE`` for more information.
