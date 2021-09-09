package rs2d.process;

import rs2d.spinlab.data.io.DatasetIO;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.study.Serie;
import rs2d.spinlab.tools.param.Param;

import java.io.IOException;

public class SaveProcess extends ProcessPluginAbstract {
    public SaveProcess() {
        super("Save Process", "Save intermediary processed data");
    }

    @Override
    public Param[] execute(Param... params) throws IOException {
        this.checkParameters(params);

        getDataset().attach(Serie.FILENAME, serie);
        DatasetIO.saveProcess(getDataset(), serie.getOutputDirectory());

        return new Param[0];
    }
}
