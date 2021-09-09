package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.PointElement;
import rs2d.spinlab.data.util.DatasetAxis;
import rs2d.spinlab.data.util.iterators.PointDatasetIterator;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;

public class Amplitude extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public Amplitude() {
        super("Amplitude", "Compute the amplitude");
    }

    public Amplitude(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        BooleanParam phaseCorrection = new BooleanParam("Sum Of Square", true,
                "Do the sum of square of the signal coming from all receivers.");
        return new Param[]{phaseCorrection};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        boolean sumOfSquare = ((Boolean) params[0].getValue());
        this.executeAmplitude(sumOfSquare);
        return new Param[0];
    }

    //
    // Amplitude process
    //

    /**
     * Execute the amplitude process.
     * @param sumOfSquare Sum of square flag. If true, amplitude will be computed from all receivers; otherwise amplitudes
     *                    computed separately.
     */
    private void executeAmplitude(boolean sumOfSquare) {
        if (((Number) this.getDataset().getHeader().getParam(DefaultParams.RECEIVER_COUNT).getValue()).intValue() > 1 && sumOfSquare) {
            new PointDatasetIterator(this.getDataset())
                    .map(point -> {
                        point.real = point.real * point.real + point.imag * point.imag;
                        point.imag = 0;
                        return point;
                    })
                    .reduce(new PointElement(), (point1, point2) -> {
                        point1.real += point2.real;
                        return point1;
                    }, DatasetAxis.RECEIVER)
                    .map(point -> {
                        point.real = (float) Math.sqrt(point.real);
                        return point;
                    })
                    .restrictMemoryToRegion();
        } else {
            new PointDatasetIterator(this.getDataset()).map(point -> {
                point.real = (float) Math.sqrt(point.real * point.real + point.imag * point.imag);
                point.imag = 0;
                return point;
            });
        }
    }
}
