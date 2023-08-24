import edu.rit.se.testsmells.MappingDetector;
import entity.ClassEntity;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.github.GHFileNotFoundException;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import testsmell.TestFile;
import testsmell.TestSmellDetector;
import thresholds.DefaultThresholds;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class Main {

    static List<edu.rit.se.testsmells.TestFile> testFileSimplified;
    static TestSmellDetector testSmellDetector = new TestSmellDetector(new DefaultThresholds());
    static TsDetectWritter resultsWriter;
    private static Path path = null;

    public static void main(String[] args) throws IOException {

        if (args.length != 4) {
            System.out.println("Please provide exactly 4 arguments: csv file, tsDetector output folder, " +
                    "refactoringminer output folder, and folder to clone the projects");
            System.exit(1);  // Exit with a non-zero status code
        }

        // Retrieving the projects data
        Map<String, List<Project>> commitsById = null;
        if (!args[0].isEmpty()) {
            File inputFile = new File(args[0]);
            if (!inputFile.exists() || inputFile.isDirectory()) {
                System.out.println("Please provide a valid file with the projects info");
                return;
            }
            else{
                commitsById = readCSV(inputFile);
            }
        }

        File tsDetectOutput = null;
        File tRefOutput = null;
        File cloneOutput = null;
        if(!args[1].isEmpty() && !args[2].isEmpty() && !args[3].isEmpty()){
            tsDetectOutput = new File(args[1]);
            tRefOutput = new File(args[2]);
            cloneOutput = new File(args[3]);
            if (!tsDetectOutput.exists() || !tsDetectOutput.isDirectory()) {
                System.out.println("Please provide a valid folder to save the Test Smell Detector Output");
                return;
            }
            if (!tRefOutput.exists() || !tRefOutput.isDirectory()) {
                System.out.println("Please provide a valid folder to save the TestRefactoringMiner Output");
                return;
            }
            if (!cloneOutput.exists() || !cloneOutput.isDirectory()) {
                System.out.println("Please provide a valid folder to clone the projects");
                return;
            }
        }


        // Clone the project repository
        for (Map.Entry<String, List<Project>> entry : commitsById.entrySet()) {
            String id = entry.getKey();
            List<Project> commitList = entry.getValue();

            // Clone project
            File repository = cloningRepository(id, cloneOutput.getAbsolutePath());

            // Configuring TestRefactoringMiner
            String fileName = id.replace("/", "-");
            processJSONoption(tRefOutput.getAbsolutePath()+"/"+fileName+".json");
            startJSON();

            // Configuring tsDetect
            resultsWriter = TsDetectWritter.createResultsWriter(tsDetectOutput.getAbsolutePath()+"/"+fileName);
            List<String> columnNames;

            columnNames = testSmellDetector.getTestSmellNames();
            columnNames.add(0, "App");
            columnNames.add(1, "SHA");
            columnNames.add(2, "TestClass");
            columnNames.add(3, "TestFilePath");
            columnNames.add(4, "ProductionFilePath");
            columnNames.add(5, "RelativeTestFilePath");
            columnNames.add(6, "RelativeProductionFilePath");
            columnNames.add(7, "NumberOfMethods");

            resultsWriter.writeColumnName(columnNames);

            System.out.println("-------------- START PROJECT --------------");
            int betweenJ = 0;
            for (int i = 0; i < commitList.size(); i++) {
                System.out.println("Analyzing "+repository+ "   "+ commitList.get(i).getSha());
                runTestRefactoringMiner(repository, commitList.get(i).getSha());
                if (betweenJ < commitList.size()-1){
                    betweenJSON();
                }
                betweenJ++;
                runTsDetect(id, repository.getAbsolutePath(), commitList.get(i).getSha());
            }
            endJSON();
            System.out.println("-------------- END PROJECT --------------");



        }



    }

    private static void runTsDetect(String id, String rootDirectory, String sha) {
        //File repository = new File(rootDirectory);
        TestFileDetector testFileDetector = TestFileDetector.createTestFileDetector();
        ClassEntity classEntity;

        try {
            Git git = Git.open(new File(rootDirectory));
            // Checkout to the desired commit
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            git.checkout().setForced(true).setName(sha).call();
            //recursively identify all 'java' files in the specified directory
            try {
                Util.writeOperationLogEntry("Identify all 'java' test files", Util.OperationStatus.Started);

                FileWalker fw = new FileWalker();
                List<Path> files = fw.getJavaTestFiles(rootDirectory, true);
                Util.writeOperationLogEntry("Identify all 'java' test files", Util.OperationStatus.Completed);


                //foreach of the identified 'java' files, obtain details about the methods that they contain
                MappingDetector mappingDetector;
                testFileSimplified = new ArrayList<>();
                Util.writeOperationLogEntry("Obtain method details", Util.OperationStatus.Started);
                for (Path file : files) {
                    try {
                        classEntity = testFileDetector.runAnalysis(file);
                        mappingDetector = new MappingDetector();
                        System.out.println(classEntity.getFilePath());
                        testFileSimplified.add(mappingDetector.detectMapping(rootDirectory
                                + "," + classEntity.getFilePath()));
                    } catch (Exception e) {
                        Util.writeException(e, "File: " + file.toAbsolutePath().toString());
                    }
                }
                Util.writeOperationLogEntry("Obtain method details", Util.OperationStatus.Completed);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // Read the input file and build the TestFile objects
            List<testsmell.TestFile> testFiles = new ArrayList<>();
            for (int i = 0; i < testFileSimplified.size(); i++) {
                String nameProd = "";
                if (!testFileSimplified.get(i).getTestFilePath().isEmpty()) {
                    if (!testFileSimplified.get(i).getProductionFilePath().isEmpty()) {
                        nameProd = testFileSimplified.get(i).getProductionFilePath();
                    }
                }
                testFiles.add(new TestFile(id, testFileSimplified.get(i).getTestFilePath(), nameProd));
            }

            List<String> columnValues;

            // Iterate through all test files to detect smells and then write the output
            try {
                TestFile tempFile;
                for (TestFile file : testFiles) {

                    //detect smells
                    tempFile = testSmellDetector.detectSmells(file);

                    //write output
                    columnValues = new ArrayList<>();
                    columnValues.add(id);
                    columnValues.add(sha);
                    columnValues.add(file.getTestFileName());
                    columnValues.add(file.getTestFilePath());
                    columnValues.add(file.getProductionFilePath());
                    columnValues.add(file.getRelativeTestFilePath());
                    columnValues.add(file.getRelativeProductionFilePath());
                    columnValues.add(String.valueOf(file.getNumberOfTestMethods()));
                    for (testsmell.AbstractSmell smell : tempFile.getTestSmells()) {
                        try {
                            columnValues.add(String.valueOf(smell.getNumberOfSmellyTests()));
                        } catch (NullPointerException e) {
                            columnValues.add("");
                        }
                    }
                    resultsWriter.writeLine(columnValues);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private static void runTestRefactoringMiner(File repositoryFile, String commitId) {
        System.out.println(repositoryFile.getAbsolutePath() + "   "+ commitId);


        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(repositoryFile.getAbsolutePath())) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
            detector.detectAtCommit(repo, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, refactorings);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static void startJSON() {
        if(path != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("{").append("\n");
            sb.append("\"").append("commits").append("\"").append(": ");
            sb.append("[").append("\n");
            try {
                Files.write(path, sb.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void betweenJSON() {
        if (path != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(",").append("\n");

            try {
                Files.write(path, sb.toString().getBytes(), new OpenOption[]{StandardOpenOption.APPEND});
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

    }

    private static void endJSON() {
        if(path != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("]").append("\n");
            sb.append("}");
            try {
                Files.write(path, sb.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void processJSONoption(String pathName) {
        path = Paths.get(pathName);
        try {
            if(Files.exists(path)) {
                Files.delete(path);
            }
            if(Files.notExists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void commitJSON(String cloneURL, String currentCommitId, List<Refactoring> refactoringsAtRevision) {
        if(path != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("{").append("\n");
            sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"").append(cloneURL).append("\"").append(",").append("\n");
            sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"").append(currentCommitId).append("\"").append(",").append("\n");
            String url = GitHistoryRefactoringMinerImpl.extractCommitURL(cloneURL, currentCommitId);
            sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"").append(url).append("\"").append(",").append("\n");
            sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
            sb.append("[");
            int counter = 0;
            for(Refactoring refactoring : refactoringsAtRevision) {
                sb.append(refactoring.toJSON());
                if(counter < refactoringsAtRevision.size()-1) {
                    sb.append(",");
                }
                sb.append("\n");
                counter++;
            }
            sb.append("]").append("\n");
            sb.append("}");
            try {
                Files.write(path, sb.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read csv file with the projects info
     * String[] parts refers to the csv columns: ID, OWNER, REPO, TAG, SHA
     * */
    private static Map<String, List<Project>> readCSV(File file) {
        Map<String, List<Project>> commitsById = new HashMap<>();

        try {
            Scanner scanner = new Scanner(file);
            scanner.nextLine(); // Discard header
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                String id = parts[1]+"/"+parts[2];
                List<Project> commitList = commitsById.computeIfAbsent(id,
                        k -> new ArrayList<>());
                commitList.add(new Project(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return commitsById;
    }

    private static File cloningRepository(String repoName, String cloneOutput){
        String diretoryName = cloneOutput+"/"+repoName;
        File directory = new File(diretoryName);

        GHRepository repository = null;
        if(!directory.exists()){
            directory.mkdir();
            try {
                GitHub github  = GitHub.connectAnonymously(); //GitHub.connectUsingOAuth("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"); // Add your key
                repository = github.getRepository(repoName);
                System.out.println("Cloning : " + repository.getName());

                Git.cloneRepository().setURI(repository.getHttpTransportUrl())
                        .setBranch(repository.getDefaultBranch())
                        .setDirectory(directory)
                        .call();
            } catch (GHFileNotFoundException e) {
                System.err.println(repository.getName() + " not found!");
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (GitAPIException e) {
                System.out.println("Error: " + repository.getFullName());
                e.printStackTrace();
            }
        }
        return directory;
    }
}

