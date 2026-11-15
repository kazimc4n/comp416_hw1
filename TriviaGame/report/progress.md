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
