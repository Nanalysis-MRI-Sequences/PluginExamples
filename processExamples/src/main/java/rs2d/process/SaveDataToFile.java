package rs2d.process;

import rs2d.commons.log.Log;
import rs2d.spinlab.data.Data;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.preferences.SpinlabPrefs;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.TextParam;

import java.io.*;

public class SaveDataToFile extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public SaveDataToFile() {
        super("SaveDataToFile", "Save the data into a file");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        return new Param[]{new TextParam("Path",
            SpinlabPrefs.getPreferences().getDatasetDirectory().getAbsolutePath(),
            "The path for the file")};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        String path = ((TextParam) params[0]).getValue();
        this.executeSaveDataToFile(path);
        return new Param[0];
    }

    //
    // SaveDataToFile process
    //

    /**
     * Execute SaveDataToFile process.
     * @param path Path where data will be saved.
     */
    private void executeSaveDataToFile(String path) {
        try (OutputStream out = new FileOutputStream(path)) {
            int receiversCount = this.getDataset().getHeader().getNumberParam(DefaultParams.RECEIVER_COUNT.name()).getValue().intValue();
            BufferedOutputStream buffer = new BufferedOutputStream(out);
            OutputStreamWriter writer = new OutputStreamWriter(buffer);

            for (int r = 0; r < receiversCount; r++) {
                SaveDataToFile.writeDataToStream(writer, this.getDataset().getData(r));
            }

            writer.flush();
        } catch (IOException ex) {
            Log.error(this.getClass(), ex);
        }
    }

    /**
     * Write the data to the specified output stream.
     * @param writer Output stream in which data will be written.
     * @param data Data to write.
     * @throws IOException In case anything goes wrong during writing to the stream.
     */
    private static void writeDataToStream(OutputStreamWriter writer, Data data) throws IOException {
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();

        for (int l = 0; l < real.length; l++) {
            for (int k = 0; k < real[0].length; k++) {
                for (int j = 0; j < real[0][0].length; j++) {
                    for (int i = 0; i < real[0][0][0].length; i++) {
                        writer.write("" + real[l][k][j][i] + " ");
                        writer.write("" + imag[l][k][j][i] + "\n");
                    }
                }
            }
        }
    }
}
