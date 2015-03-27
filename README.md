# Sample Java CmisClient
##Abstract
As I was asked by some customers to provide some code samples on how to access our CMIS interface I decided to make a very small and simple Java Client to demonstrate the basic functions.
#Sample Java Client
A very simple client implementation which uses Apache Chemistry OpenCMIS is available on GitHub [here](https://github.com/shomeier/cmis-client-sample).

It demonstrates the main use cases like
* connect to the server
* ask for folders and documents.
* get the metadata of a document
* get the content stream of a document and save it to local hard disk
* get the renditions (= previews) of a document, its metadata and save its content stream to local hard disk

The overall code consists only of about 200 lines.

#Setup
First simply clone the repository https://github.com/shomeier/cmis-client-sample.git and import the included Eclipse project.
Then provide the appropriate infos in */src/main/ressources/config.properties*:
```
user = <username>
password = <password>
url = <http_url_of_the_cmis_server>
temp_path = <path_where_to_store_the_tmp_content_streams>
```

