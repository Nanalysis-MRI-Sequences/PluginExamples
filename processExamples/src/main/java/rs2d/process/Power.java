package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.util.iterators.PointDatasetIterator;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.Param;

public class Power extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public Power() {
        super("Power", "Display the power spectrum");
    }

    public Power(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        this.executePower();
        return new Param[0];
    }

    //
    // Power process
    //

    /**
     * Execute Power process.
     */
    private void executePower() {
        new PointDatasetIterator(this.getDataset()).map(point -> {
            point.real = point.real * point.real + point.imag * point.imag;
            point.imag = 0;
            return point;
        });
    }
}
