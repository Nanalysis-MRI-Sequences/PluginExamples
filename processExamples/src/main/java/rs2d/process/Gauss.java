package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.util.iterators.PointDatasetIteratorWithIndex;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.NumberEnum;
import rs2d.spinlab.tools.param.NumberParam;
import rs2d.spinlab.tools.param.Param;

public class Gauss extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public Gauss() {
        super("Gaussian", "Gaussian apodization.");
    }

    public Gauss(DataSetInterface dataset) {
        this();
        setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        NumberParam width = new NumberParam("Width", 0.1, NumberEnum.Double);
        NumberParam offset = new NumberParam("Offset", 0, NumberEnum.Integer);
        return new Param[]{width, offset};
    }

    @Override
    public Param[] execute(Param... params) {
        checkParameters(params);
        double width = ((NumberParam) params[0]).getValue().doubleValue();
        int offset = ((NumberParam) params[1]).getValue().intValue();
        executeGauss(width, offset);
        return new Param[0];
    }

    //
    // Gauss process
    //

    /**
     * Execute the Gauss process.
     * @param width Width of the Gaussian.
     * @param offset Offset of the Gaussian.
     */
    private void executeGauss(double width, int offset) {
        int matrix1D = getDataset().getHeader().getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();
        double center = (matrix1D + offset) / 2d;
        double squaredWidth = width * width;

        new PointDatasetIteratorWithIndex(getDataset()).map(point -> {
            double x = (center - point.column) / matrix1D;
            double factor = Math.exp(- (x * x) / squaredWidth);
            point.real *= factor;
            point.imag *= factor;
            return point;
        });
    }
}
