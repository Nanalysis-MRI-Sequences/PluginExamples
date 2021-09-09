package rs2d.process;

import rs2d.commons.log.Log;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.exception.SpinlabException;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.NumberEnum;
import rs2d.spinlab.tools.param.NumberParam;
import rs2d.spinlab.tools.param.Param;

public class Split extends ProcessPluginAbstract {
    //
    // Constructor
    //

    public Split() {
        super("Split 1D", "Split the data");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        return new Param[]{new NumberParam("Size", 0, NumberEnum.Integer)};
    }

    @Override
    public Param[] execute(Param... params) throws SpinlabException {
        this.checkParameters(params);

        Header header = this.getDataset().getHeader();
        int matrix2D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_3D).getValue().intValue();

        if(matrix2D != 1) {
            throw new SpinlabException("Unable to split, there are already several 2Ds: MATRIX_DIMENSION_2D=" + matrix2D);
        }

        int matrix4D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_4D).getValue().intValue();
        int matrix3D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_3D).getValue().intValue();
        int matrix1D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();

        int splitSize = ((NumberParam) params[0]).getValue().intValue();
        int final2D = matrix1D / splitSize;

        Log.debug(this.getClass(), "original size: [%d][%d][%d][%d]", matrix4D, matrix3D, matrix2D, matrix1D);
        Log.debug(this.getClass(), "new size: [%d][%d][%d][%d]", matrix4D, matrix3D, final2D, splitSize);

        this.getDataset().getData().forEach(data -> {
            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            float[][][][] newReal = new float[matrix4D][matrix3D][final2D][splitSize];
            float[][][][] newImag = new float[matrix4D][matrix3D][final2D][splitSize];

            for (int l = 0; l < matrix4D; l++) {
                for (int k = 0; k < matrix3D; k++) {
                    for (int j = 0; j < final2D; j++) {
                        System.arraycopy(real[l][k][0], j * splitSize, newReal[l][k][j], 0, splitSize);
                        System.arraycopy(imag[l][k][0], j * splitSize, newImag[l][k][j], 0, splitSize);
                    }
                }
            }

            data.setRealPart(newReal);
            data.setImaginaryPart(newImag);
        });

        header.getParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(splitSize);
        header.getParam(DefaultParams.MATRIX_DIMENSION_2D).setValue(final2D);
        
        return new Param[0];
    }
}
