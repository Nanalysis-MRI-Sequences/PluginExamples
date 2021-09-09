package rs2d.process;

import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.Param;

public class RemoveImaginaryPart extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public RemoveImaginaryPart() {
        super("RemoveImaginaryPart", "Remove imaginary part");
    }

    //
    // Implementation of PluginProcess
    //

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        this.executeRemoveImaginaryPart();
        return new Param[0];
    }

    //
    // RemoveImaginaryPart process
    //

    /**
     * Execute RemoveImaginaryPart process.
     */
    private void executeRemoveImaginaryPart() {
        this.getDataset().getData().forEach(data -> {
            float[][][][] imag = data.getImaginaryPart();

            for (int l = 0; l < imag.length; l++) {
                for (int k = 0; k < imag[0].length; k++) {
                    for (int j = 0; j < imag[0][0].length; j++) {
                        for (int i = 0; i < imag[0][0][0].length; i++) {
                            imag[l][k][j][i] = 0;
                        }
                    }
                }
            }
        });
    }
}
