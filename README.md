# OpenML Extension for RapidMiner

## Installation
1. Install RapidMiner Studio (e.g. with educational license)
2. Install Java JDK 1.8 or higher 
3. Clone/download this repo
4. Execute `./gradlew installExtension` from inside the "OpenmlConnector" folder

## Usage
1. Open RapidMiner Studio and find the OpenML operators in the "Extensions" operator folder.
2. Use the "Download Openml task" to download a task with a certain id. 
3. Create a connection with "OpenML connection" 
  - Set the `domain` parameter to "https://www.openml.org/"
  - Set the `API key` parameter to your API key (can be found on your OpenML profile page)
4. Link that to the "Execute Openml task" operator, which is a subprocess. 
  - Double click it and add your process in there
5. Upload the results using the "Upload Openml task" operator with the same connection
