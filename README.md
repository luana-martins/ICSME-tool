# ICSME-tool

## External resources

### TestSmellDetector

TestSmellDetector requires the execution of two other modules to generate a CSV file that specifies the list of test files (and their associated production file). As the modules are not available on the Maven repository, you will need to install them through the steps:

1. Clone the repositories hosting each module:
   
   ```cmd
   https://github.com/TestSmells/TestFileDetector.git
   https://github.com/TestSmells/TestFileMapping.git
   https://github.com/TestSmells/TestSmellDetector.git
   ```

   Alternatively, you can also go to `ICSME-tool/resource/` and download each module's `.zip` files.
   
3. Go to the root folder of each repository in your machine and run:

   ```cmd
   mvn install
   ```
4. As a result, the `.m2` directory on your PC, associated with Apache Maven, will contain the three new repositories. e.g., `C:\Users\luana\.m2\repository\edu\rit\se\testsmells\TestFileDetector`, `C:\Users\luana\.m2\repository\edu\rit\se\testsmells\TestFileMapping`, and `C:\Users\luana\.m2\repository\edu\rit\se\testsmells\TestSmellDetector`.

5. All the modules are included on `pom.xml` as dependencies.

### TestRefactoringMiner

TestRefactoringMiner builds on top of RefactoringMiner 2.3.2. We add the RefactoringMiner as a dependency on `pom.xml`, and we also provide the `refactoring-miner-2.3.2.jar` in the `ICSME-tool/resource/`. You can add the dependency in the project in two different ways.

1. As the `pom.xml` already creates a folder to the RefactoringMiner's repository, you can just replace the jar `.m2` directory, e.g., `C:\Users\luana\.m2\repository\com\github\tsantalis\refactoring-miner\2.3.2`

2. Alternatively, you can add the `jar` as an external library in your IDE. If you are using IntelliJ, follow the steps:
  
   1. Remove the dependency from `pom.xml`
   2. Right-click on the project's name
   Select the `Open Module Settings` option
   3. Go to `Libraries`
   4. On the top-left, select the `+` icon
   5. Select the `Java` option
   6. Select the `jar` file
      

## Configure the output folders

Please, create on the root folder of the project three other folders:
1. `clone` - it will host all the repositories' clones
2. `resultsTRefMiner` - it will receive the outputs from TestRefactoringMiner
3. `resultsTsDetect` - it will receive the outputs from tsDetect

