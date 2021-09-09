package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.EnumParamLiberty;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.utility.MathUtility;

public class ZIP extends ProcessPluginAbstract {
    private static final boolean DEFAULT_ZIP = true;

    //
    // Construction
    //

    public ZIP() {
        super("ZIP", "Change the size to the next power of 2 and if zip = true, double the size");
    }

    public ZIP(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Properties
    //

    @Override
    public EnumParamLiberty[] getParamsLiberty() {
        return new EnumParamLiberty[]{EnumParamLiberty.MustBeAssigned};
    }

    @Override
    public Param[] getParam() {
        return new Param[]{new BooleanParam("ZIP", DEFAULT_ZIP, "Multiply the matrix dimensions by 2")};
    }

    //
    // Public API
    //

    @Override
    public Param[] execute(Param... params) {
        checkParameters(params);

        boolean doubleSize = (Boolean) params[0].getValue();
        executeZIP(doubleSize);

        return new Param[0];
    }

    //
    // Internal methods
    //

    /**
     * Execute ZIP process.
     * @param doubleSize Flag indicating if the size will be doubled or not.
     */
    private void executeZIP(boolean doubleSize) {
        DataSetInterface dataset = getDataset();
        Header header = dataset.getHeader();

        int matrix1D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D.name()).getValue().intValue();

        int nextPowerOf2In1D = MathUtility.nextPowerOf2(matrix1D);
        int newMatrix1D = doubleSize ? nextPowerOf2In1D * 2 : nextPowerOf2In1D;
        int offset = (newMatrix1D - matrix1D) / 2;

        dataset.getData().forEach(data -> ZF.zeroFilling(data, offset, matrix1D, newMatrix1D));
        header.getParam(DefaultParams.MATRIX_DIMENSION_1D.name()).setValue(newMatrix1D);
    }
}
