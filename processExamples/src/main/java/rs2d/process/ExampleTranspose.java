package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.ListNumberParam;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.TextParam;
import rs2d.spinlab.tools.utility.MathUtility;

import java.util.Arrays;

public class ExampleTranspose extends ProcessPluginAbstract {
    public static final String DIRECT = "Direct";
    public static final String INDIRECT = "Indirect";

    //
    // Constructors
    //

    public ExampleTranspose() {
        super("Transpose", "Transpose the current dataset");
    }

    public ExampleTranspose(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        TextParam transposeOrder = new TextParam("Transpose Order", ExampleTranspose.DIRECT, "The order of the transposition");
        transposeOrder.setSuggestedValues(Arrays.asList(ExampleTranspose.DIRECT, ExampleTranspose.INDIRECT));
        transposeOrder.setRestrictedToSuggested(true);
        return new Param[]{transposeOrder};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        String directDim = (String) params[0].getValue();
        this.executeTranspose(directDim.equals(ExampleTranspose.DIRECT));
        return new Param[0];
    }

    //
    // Transpose process
    //

    /**
     * Execute transpose process.
     * @param direct True if direct order is wanted; false for the indirect order.
     */
    private void executeTranspose(boolean direct) {
        Header header = this.getDataset().getHeader();
        int receiverCounts = header.getNumberParam(DefaultParams.RECEIVER_COUNT).getValue().intValue();
        int matrix3D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_3D).getValue().intValue();
        int matrix2D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_2D).getValue().intValue();
        int matrix1D = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();

        if (!header.hasParam(DefaultParams.STATE)) {
            header.putParam(new DefaultParams().getParam(DefaultParams.STATE));
        }

        if (!(header.getParam(DefaultParams.PHASE_0) instanceof ListNumberParam)) {
            header.putParam(new DefaultParams().getParam(DefaultParams.PHASE_0));
            header.putParam(new DefaultParams().getParam(DefaultParams.PHASE_1));
        }

        ExampleTranspose.transposeParameters(header.getListNumberParam(DefaultParams.STATE), direct);
        ExampleTranspose.transposeParameters(header.getListNumberParam(DefaultParams.PHASE_0), direct);
        ExampleTranspose.transposeParameters(header.getListNumberParam(DefaultParams.PHASE_1), direct);

        if (direct) {
            header.getParam(DefaultParams.MATRIX_DIMENSION_3D).setValue(matrix1D);
            header.getParam(DefaultParams.MATRIX_DIMENSION_2D).setValue(matrix3D);
            header.getParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(matrix2D);
        } else {
            header.getParam(DefaultParams.MATRIX_DIMENSION_3D).setValue(matrix2D);
            header.getParam(DefaultParams.MATRIX_DIMENSION_2D).setValue(matrix1D);
            header.getParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(matrix3D);
        }

        for (int rec = 0; rec < receiverCounts; rec++) {
            float[][][][][] transpose = MathUtility.transpose(direct,
                    this.getDataset().getData(rec).getRealPart(),
                    this.getDataset().getData(rec).getImaginaryPart());
            this.getDataset().setData(transpose[0], transpose[1], rec);
        }
    }

    /**
     * Transpose the dimensional parameters.
     * @param dimensionalParam Dimensional parameters.
     * @param direct True if direct order is wanted; false for the indirect order.
     */
    private static void transposeParameters(ListNumberParam dimensionalParam, boolean direct) {
        Number old1D = dimensionalParam.getValueAt(0, 0);
        Number old2D = dimensionalParam.getValueAt(1, 0);
        Number old3D = dimensionalParam.getValueAt(2, 0);

        if(direct) {
            dimensionalParam.setValueAt(2, old1D);
            dimensionalParam.setValueAt(1, old3D);
            dimensionalParam.setValueAt(0, old2D);
        } else {
            dimensionalParam.setValueAt(2, old2D);
            dimensionalParam.setValueAt(1, old1D);
            dimensionalParam.setValueAt(0, old3D);
        }
    }
}
