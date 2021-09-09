package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.data.util.iterators.PointDatasetIteratorWithIndex;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.MriDefaultParams;
import rs2d.spinlab.tools.param.Param;

public class SignAlternation extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public SignAlternation() {
        super("SignAlternation", "SignAlternation of data");
    }

    public SignAlternation(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        this.executeSignAlternation();
        return new Param[0];
    }

    //
    // SignAlternation process
    //

    /**
     * Execute SignAlternation process.
     */
    private void executeSignAlternation() {
        Header header = this.getDataset().getHeader();
        boolean multiPlanar = header.hasParam(MriDefaultParams.MULTI_PLANAR_EXCITATION.name()) ?
                header.getBooleanParam(MriDefaultParams.MULTI_PLANAR_EXCITATION.name()).getValue() : false;

        new PointDatasetIteratorWithIndex(this.getDataset()).map(point -> {
            int sign = (multiPlanar || (point.slice % 2) == 0 ? 1 : -1) * ((point.row % 2) == 0 ? 1 : -1) * ((point.column % 2) == 0 ? 1 : -1);
            point.real *= sign;
            point.imag *= sign;
            return point;
        });
    }
}
