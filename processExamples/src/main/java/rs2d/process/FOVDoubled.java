package rs2d.process;

import rs2d.commons.log.Log;
import rs2d.spinlab.data.Data;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;

public class FOVDoubled extends ProcessPluginAbstract {
    //
    // Construction
    //

    public FOVDoubled() {
        super("FOVDoubled", "correct for FOV doubled");
    }


    //
    // Implements of process plugins
    //

    @Override
    public Param[] getParam() {

        return new Param[]{};
    }

    @Override
    public Param[] getResult() {
        return new Param[0];
    }

    /**
     * Execute the ExtractCol process.
     * if FOV_DOUBLED = true keep only the central half of the data 1D
     */
    @Override
    public Param[] execute(Param... params) {
        checkParameters(params);

        boolean isFOVDoubled = false;

        if (getDataset().getHeader().hasParam("FOV_DOUBLED")) {
            isFOVDoubled = ((BooleanParam) getDataset().getHeader().getParam("FOV_DOUBLED")).getValue();
        }

        int matrix1D = getDataset().getHeader().getNumberParam(DefaultParams.MATRIX_DIMENSION_1D.name()).intValue();

        DataSetInterface dataset = getDataset();
        Header header = dataset.getHeader();

        if (isFOVDoubled) {
            dataset.getData().forEach(FOVDoubled::cutHalf);
            header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(matrix1D / 2);
        }

        return new Param[0];
    }

    static void cutHalf(Data data) {
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();
        int size = real[0][0][0].length / 2;
        int offset = real[0][0][0].length / 4;

        float[][][][] newReal = new float[real.length][real[0].length][real[0][0].length][size];
        float[][][][] newImag = new float[imag.length][imag[0].length][imag[0][0].length][size];
        Log.debug(FOVDoubled.class, "offset: %d; size: %d.", offset, size);

        for (int l = 0; l < real.length; l++) {
            for (int k = 0; k < real[0].length; k++) {
                for (int j = 0; j < real[0][0].length; j++) {
                    Log.trace(FOVDoubled.class, "copying data at location (%d, %d, %d)", l, k, j);
                    System.arraycopy(real[l][k][j], offset, newReal[l][k][j], 0, size);
                    System.arraycopy(imag[l][k][j], offset, newImag[l][k][j], 0, size);
                }
            }
        }

        data.setRealPart(newReal);
        data.setImaginaryPart(newImag);
    }

}
