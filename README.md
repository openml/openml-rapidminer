# OpenML Extension for RapidMiner

## Installation
1. Install RapidMiner Studio (e.g. with educational license)
2. Install Java JDK 1.8 or higher 
3. Clone/download this repo
4. Execute `./gradlew installExtension` from inside the "OpenmlConnector" folder
![installing connector](https://user-images.githubusercontent.com/24679211/38961120-60728180-4367-11e8-8fe3-29dba33c3012.png)

## Usage
1. Open RapidMiner Studio and find the OpenML operators in the "Extensions" operator folder.
![openml operators](https://user-images.githubusercontent.com/24679211/38961142-7cd3d5b8-4367-11e8-8522-43fff584160a.png)
2. Use the "Download Openml task" to download a task with a certain id. 
3. Create a connection with "OpenML connection" 
  - Set the `url` parameter to "https://www.openml.org/"
  - Set the `API key` parameter to your API key (can be found on your OpenML profile page)
![openmlconnection](https://user-images.githubusercontent.com/24679211/38961154-8b9acd18-4367-11e8-9fa9-806998939198.png)
![opeml credentials](https://user-images.githubusercontent.com/24679211/38961166-95f5d1ae-4367-11e8-81cd-f2b45ce6f88b.png)
4. Link that to the "Execute Openml task" operator, which is a subprocess. 
  - Double click it and add your process in there
![model operator](https://user-images.githubusercontent.com/24679211/38961187-a422b562-4367-11e8-9b7f-2463372cb305.png)
5. Upload the results using the "Upload Openml task" operator with the same connection
