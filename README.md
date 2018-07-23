# Goobi viewer - Core
[![Build Status](https://travis-ci.org/intranda/goobi-viewer-core.svg?branch=master)](https://travis-ci.org/intranda/goobi-viewer-core)

> A highly flexible digital library framework - made by the Goobi developer team


## Digital library framework
The Goobi viewer is a web application that allows digitised material to be displayed in a web browser. It is used as central basis for an online digital library and offers various functionalities as browsing through digital collections, complex full-text searches, displaying images based on the IIIF standard, deliver audio and video material and a lot more. 

![alt Goobi viewer - image view of a book page](doc/images/goobi_viewer_theme_reference_image_display.png "Goobi viewer - image view of a book page")


### Metadata handling
A consistent use of standardised metadata format (e.g. METS or LIDO) allows to present data from various sources. Additionally the viewer can be fully integrated into Goobi, the popular open-source workflow management software for digitisation projects, thus creating a seamless transition from the book to the web presentation.

### Content and access
The Goobi viewer handles the entire visualisation of the digitised material. Based on standardised metadata formats, it not only displays the material but also offers website visitors a range of useful tools that help them to navigate and even to contribute their own content – for example a page turning/location function and quick links from the digital index. 
Content inside of the Goobi viewer is available for download as small parts (e.g. individual chapters) or as a complete work in the form of archivable PDF/A files. Beside this there is a licensing system embedded to control the access to special content by granting an authorisation for example to certain IP addresses or to individual users.

### Layout
The layout of the Goobi viewer is based on a sophisticated templating engine with highly configurable skins to offer a wide range of visual display options and to allow an integration fully into a given corporate design.

### Some of the main features
These are some of the main functionalities that are provided:

- Any graphics format conversions
- Zoom and rotation at any level based on the IIIF standard
- Integration of watermarks in images (e.g. for copyright notices)
- On-the-fly generation of PDF/A files with cover sheet, table of contents and pagination sequences for individual structural elements or entire works
- Delivery of PDF files containing stored full texts from an OCR
- Live generation of thumbnail views of entire works
- No need to keep multiple image derivatives on the hard drive
- Display images and statistics
- Reproduce audio and video files
- Present and manage metadata
- Flexible and highly configurable searching and browsing mechanisms based on an Apache Solr search index
- Handling of embedded Named Entities
- Offer a range of downloads with no image display (e.g. PDF files, „Viewer without image“)
- Embedded content management to create individual pages
- Various interfaces (SRU, JSON, OAI-PMH)

## Documentation
A complete documentation of the Goobi viewer can be found using this URL:  
<http://www.intranda.com/en/digiverso/documentation/>

Please notice that the Goobi viewer was formerly known as proprietary software under the name 'intranda viewer' and is released under an open source license since June 2017. Lots of our documentation and description sites still have to be updated to represent the new product name 'Goobi viewer'.

You can find technical documentation for backend and frontend developers here:
- [Javadoc](https://intranda.github.io/goobi-viewer-core/goobi-viewer-core/doc/javadoc/index.html)
- [JSDoc](https://intranda.github.io/goobi-viewer-core/goobi-viewer-core/doc/jsdoc/index.html)

You can find a HTML output of the latest unit test runs here:
- [JUnit](https://intranda.github.io/goobi-viewer-core/goobi-viewer-core/test-reports-html/)
- [Jasmine](https://intranda.github.io/goobi-viewer-core/goobi-viewer-core/test-reports-karma/)

## Technical background

The Goobi viewer consists of multiple packages which all have to be installed and configured properly:

| Package | Function |
| ------ | ------ |
| [Goobi viewer core](https://github.com/intranda/goobi-viewer-core) | Core functionality of the viewer application|
| [Goobi viewer indexer](https://github.com/intranda/goobi-viewer-indexer) | Indexing application to fill the Solr search index with metadata information |
| [Goobi viewer connector](https://github.com/intranda/goobi-viewer-connector) | Connectors for different use cases (incl. OAI-PMH, SRU)|
| [Goobi viewer Theme Reference](https://github.com/intranda/goobi-viewer-theme-reference) | Reference Theme for the styling of the web pages for the user interface |


## Installation
The installation can be done on any operating system as the software is based on Java. A detailed explanation how to install the viewer will follow later on. In the mean time please get in touch with us via <info@intranda.com>

## Release History
Detailed release note descriptions can be found using this URL:  
<http://www.intranda.com/en/digiverso/goobi-viewer/history/>

## Developer team
intranda GmbH  
Bertha-von-Suttner-Str. 9  
37085 Göttingen  
Germany

## Contact us
If you would like to get in touch with the developers please use the following contact details:

| Contact |Address |
| ------ | ------ |
| Website | <http://www.intranda.com> |
| Mail | <info@intranda.com> |
| Twitter intranda | <http://twitter.com/intranda> |
| Twitter Goobi | <http://twitter.com/goobi> |
| Github | <https://github.com/intranda> |

## Licence
The Goobi viewer is released under the license GPL2 or later.  
Please see ``LICENSE`` for more information.


## Contributing

1. Fork it (<https://github.com/intranda/goobi-viewer-core/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Commit your changes (`git commit -am 'Add some fooBar'`)
4. Push to the branch (`git push origin feature/fooBar`)
5. Create a new Pull Request

