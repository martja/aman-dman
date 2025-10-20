# AMAN for EuroScope

An advanced arrival manager for EuroScope. Features:

- **Custom trajectory prediction**: Utilizes aircraft performance data and wind information to predict arrival times accurately.
- **Automatic sequencing**: Automatically sequences incoming aircraft based on their optimal arrival times and required separation.
- **Master and slave mode**: Supports master-slave configuration for shared AMAN data between multiple controllers.

## Getting started

### Prerequisites

- EuroScope installed on your computer.
- Java Runtime Environment (JRE) installed (version 21 or higher). This can be downloaded from [here](https://www.oracle.com/java/technologies/downloads/).

### Installation

1. Download the latest release from the [Releases section](https://github.com/EvenAR/aman-dman/releases).
2. Extract the contents of the ZIP file to a folder of your choice.
3. Load the EuroScope bridge plugin `Aman.dll` file from the extracted folder.
4. Run the .jar file to start the AMAN application.


### Screenshots

<img width="468" height="831" alt="image" src="https://github.com/user-attachments/assets/7955e095-cab4-4e4c-b841-42854d2e5113" />

<img width="795" height="590" alt="Screenshot 2025-10-04 130229" src="https://github.com/user-attachments/assets/f02cc2ed-b19c-464a-b01d-afaabd515384" />


## Development

### Java Application

[IntelliJ IDEA](https://www.jetbrains.com/idea/download/?section=windows) (Community Edition) is recommended as the IDE.

**Running the application from IntelliJ:**

1. Open Project → select the `aman-dman-client` directory.
2. Go to **Menu → Project Structure**:
   - Select JDK 21 (e.g., `temurin-21`)
   - Set **Language Level**: 21
3. Open the file `app/src/kotlin/.../Main.kt` and press the green arrow next to `fun main()` to run the project.

---

### EuroScope Bridge

If you need to make changes to the EuroScope bridge C++ plugin, [Visual Studio Community](https://visualstudio.microsoft.com/vs/community/) is the recommended IDE.

**Debugging with Visual Studio:**

1. Open the project in the `euroscope-bridge` directory (double-click `Aman.sln`).
2. In **Solution Explorer**, right-click on **Aman** → **Properties**.
3. Under the **Debugging** page, navigate to your installation directory of `EuroScope.exe` and apply the changes.
4. Run **Local Windows Debugger**.  
   If everything works correctly, a `.dll` file is written to `euroscope-bridge\Debug`.
5. Load the `.dll` plugin in EuroScope.
