## HTTP Server
Http server, implemented in Java, based on CodeCrafters' "Build your own HTTP Server" challenge.

The server reads(and writes) requests directly from an OS socket as a stream of bytes.

All HTTP functionality (status codes, headers handling, path resolution, compression) is implemented on top of the raw data. 

Avaialble endpoints:
* `GET /` will simply return 200 OK
* `GET /echo/{value}` will echo the path value in the response
* `GET /User-Agent` will return the client's user-agent

A simple file server API (with no security) can be accessed through the `/files` endpoint 
* `GET /files/{file_name}` will return the content of the file if it exists
* `POST /files/{file_name}` will upload the file which will then be accessible through the GET method

Compression is supported for any request with a header "Content-Encoding: gzip".

_Note: The server is very minimal and doesn't handle malformed requests._
