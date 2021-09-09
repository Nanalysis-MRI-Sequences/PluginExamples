package rs2d.process;

import rs2d.spinlab.data.Data;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.*;

public class ZF extends ProcessPluginAbstract {
    private static final int DEFAULT_RECONSTRUCTION_SIZE = 512;
    private static final boolean DEFAULT_SYMMETRIC = false;

    //
    // Construction
    //

    public ZF() {
        super("ZF", "Add zero at the data end to fit the specified size(value or linked to a UserParam)");
    }

    public ZF(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Properties
    //

    @Override
    public EnumParamLiberty[] getParamsLiberty() {
        return new EnumParamLiberty[]{EnumParamLiberty.MustBeAssigned, EnumParamLiberty.MustBeAssigned};
    }

    @Override
    public Param[] getParam() {
        Param reconstructionSize = new NumberParam("Reconstruction size",
                DEFAULT_RECONSTRUCTION_SIZE, NumberEnum.AcquPoint);

        Param isSymmetric = new BooleanParam("Symmetric",
                DEFAULT_SYMMETRIC, "Data are zero-filled symmetrically");

        return new Param[]{reconstructionSize, isSymmetric};
    }

    //
    // Public API
    //

    @Override
    public Param[] execute(Param... params) {
        checkParameters(params);

        int reconstructionSize = (Integer) params[0].getValue();
        boolean isSymmetric = (Boolean) params[1].getValue();
        executeZF(reconstructionSize, isSymmetric);

        return new Param[0];
    }

    //
    // Internal methods
    //

    /**
     * Execute ZF process.
     * @param reconstructionSize Reconstruction size parameter.
     * @param isSymmetric Flag indicating if the filling is symmetric or not.
     */
    private void executeZF(int reconstructionSize, boolean isSymmetric) {
        DataSetInterface dataset = getDataset();
        Header header = dataset.getHeader();

        int initialSize = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).intValue();

        if (reconstructionSize >= initialSize) {
            int offset = isSymmetric ? (reconstructionSize - initialSize) / 2 : 0;
            dataset.getData().forEach(data -> ZF.zeroFilling(data, offset, initialSize, reconstructionSize));
            header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(reconstructionSize);
        }
    }

    /**
     * Fill-in rows of data with zeros.
     * @param data Data to zero-fill.
     * @param offset Offset of the filling.
     * @param reconstructionSize New size of the first dimension.
     * @param initialSize Previous size of the first dimension.
     */
    static void zeroFilling(Data data, int offset, int initialSize, int reconstructionSize) {
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();

        float[][][][] newReal = new float[real.length][real[0].length][real[0][0].length][reconstructionSize];
        float[][][][] newImag = new float[imag.length][imag[0].length][imag[0][0].length][reconstructionSize];

        for (int l = 0; l < real.length; l++) {
            for (int k = 0; k < real[0].length; k++) {
                for (int j = 0; j < real[0][0].length; j++) {
                    System.arraycopy(real[l][k][j], 0, newReal[l][k][j], offset, initialSize);
                    System.arraycopy(imag[l][k][j], 0, newImag[l][k][j], offset, initialSize);
                }
            }
        }

        data.setRealPart(newReal);
        data.setImaginaryPart(newImag);
    }
}
