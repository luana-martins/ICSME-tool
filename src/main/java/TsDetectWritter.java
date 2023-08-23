import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;

public class TsDetectWritter {
    private String outputFile;
    private FileWriter writer;

    private TsDetectWritter(String name) throws IOException {
       this.outputFile = MessageFormat.format("{0}_{1}.{2}", name, "TestSmellDetection", "csv");
        this.writer = new FileWriter(this.outputFile, false);
    }

    public static TsDetectWritter createResultsWriter(String name) throws IOException {
        return new TsDetectWritter(name);
    }

    public void writeColumnName(List<String> columnNames) throws IOException {
        this.writeOutput(columnNames);
    }

    public void writeLine(List<String> columnValues) throws IOException {
        this.writeOutput(columnValues);
    }

    private void writeOutput(List<String> dataValues) throws IOException {
        this.writer = new FileWriter(this.outputFile, true);

        for(int i = 0; i < dataValues.size(); ++i) {
            this.writer.append(String.valueOf(dataValues.get(i)));
            if (i != dataValues.size() - 1) {
                this.writer.append(",");
            } else {
                this.writer.append(System.lineSeparator());
            }
        }

        this.writer.flush();
        this.writer.close();
    }
}
