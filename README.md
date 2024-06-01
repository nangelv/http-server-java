## HTTP Server
Http server, implemented in Java, based on CodeCrafters' "Build your own HTTP Server" challenge.

Avaialble endpoints:
* `GET /echo/{value}` will simply return 200 OK
* `GET /echo/{value}` will echo the path value in the response
* `GET /User-Agent` will return the client's user-agent

A simple file server API (with no security) can be accessed through the `/files` endpoint 
* `GET /files/{file_name}` will return the content of the file if it exists
* `POST /files/{file_name}` will upload the file which will then be accessible through the GET method

_Note: The server is very minimal and doesn't handle malformed requests._