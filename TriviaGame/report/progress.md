# Project Progress

## Step 1: Project Setup
- Created separate server and client folders.
- Compiled and ran both Java programs.
- Saved a screenshot of the setup.

Screenshot: screenshots/01-project-setup.png

## Step 2: Basic TCP Connection
- Added a server that listens on port 41601.
- Added a client that connects to localhost.
- The server accepts one connection and then stops.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/02-tcp-connection.png

## Step 3: Multiple Client Connections
- Added a loop to accept new connections.
- Added a separate handler thread for each client.
- Assigned a unique ID to each client.
- Added a 90-second socket read timeout.
- Added a manual disconnect action using Enter.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/03-multiple-clients.png

## Step 4: Protocol Reader and Writer
- Added a Protocol helper to both projects.
- Added the 416END message marker.
- Used UTF-8 byte lengths for response bodies.
- Added exact-length body reading.
- Added tests for consecutive messages and empty bodies.
- Added a UTF-8 test and simulated small body reads.
- Socket integration is not implemented in this step.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/04-protocol-tests.png

## Step 5: Commands and Responses
- Connected the Protocol helper to the server and client.
- Used separate threads for console input and server responses.
- Added a synchronized server send method.
- Added /exit to close the connection.
- Returned 400 for unknown or not yet supported commands.
- Challenge commands are not implemented yet.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/05-command-response.png

## Step 6: Category Cache
- Added Gson to read JSON data.
- Loaded category names and IDs when the server starts.
- Stored categories in memory.
- Added a category ID lookup method.
- Stopped startup if category loading fails.
- Kept the existing client connection behavior.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/06-category-cache.png
