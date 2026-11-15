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

## Step 7: Create Command Parsing
- Added a parser for the /create command.
- Supported quoted category names.
- Checked required options, difficulty and question count.
- Rejected duplicate options and missing closing quotes.
- Added a small parser test.
- Challenge creation is not connected to the server yet.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/07-create-command.png

## Step 8: Question Loading
- Added a Question class.
- Added question loading to OpenTdbService.
- Requested multiple-choice questions.
- Decoded question text and answers using Java Base64.
- Combined and shuffled answer options.
- Checked the API response code and question count.
- Added a live API test.
- Challenge creation is not connected yet.

Test status: Waiting for test confirmation.
Screenshot to save: screenshots/08-question-loading.png

## Step 9: Challenge Creation
- Added a challenge manager thread.
- Added a blocking queue for creation requests.
- Added a thread-safe challenge registry.
- Added a separate thread and request queue for each challenge.
- Connected /create to command validation and question loading.
- Registered challenges only after questions were loaded.
- Returned the challenge ID after successful creation.
- Added cleanup when the creator disconnects.
- Joining and gameplay are not implemented yet.

Check status: Waiting for run confirmation.
Screenshot to save: screenshots/09-create-challenge.png

## Step 10: List Challenges
- Added the /list command.
- Added optional category and difficulty filters.
- Reused the existing command tokenizer.
- Read challenge information from the in-memory registry.
- Displayed the challenge ID, category, difficulty and player count.
- Joining is not implemented yet.

Check status: Waiting for run confirmation.
Screenshot to save: screenshots/10-list-challenges.png
