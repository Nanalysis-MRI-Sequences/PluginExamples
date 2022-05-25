package rs2d.process;

import rs2d.commons.log.Log;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.*;


public class ExamplePermute extends ProcessPluginAbstract {

    private static final int DEFAULT_1ST_DIMENSION = 0;
    private static final int DEFAULT_2ND_DIMENSION = 3;
    //
    // Constructors
    //

    public ExamplePermute() {
        super("Permute", "Permute two dimensions of the current dataset");
    }

    public ExamplePermute(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {


        NumberParam Dimension1 = new NumberParam("First Dimension",
                DEFAULT_1ST_DIMENSION, NumberEnum.Integer, "First dimension to be permuted [0 - 3]");
        NumberParam Dimension2 = new NumberParam("Second Dimension",
                DEFAULT_2ND_DIMENSION, NumberEnum.Integer, "Second dimension to be permuted [0 - 3]");


        return new Param[]{Dimension1, Dimension2};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        int Dimension1 = (Integer) params[0].getValue();
        int Dimension2 = (Integer) params[1].getValue();
        this.executePermute(Dimension1, Dimension2);
        return new Param[0];
    }

    //
    // Transpose process
    //

    /**
     * Execute transpose process.
     *
     * @param dim1 dimension to be permuted
     * @param dim2 dimension to be permuted.
     */
    private void executePermute(int dim1, int dim2) {
        if (dim1 > 3 || dim1 < 0 || dim2 > 3 || dim2 < 0) {
            Log.error(getClass(),
                    "The dimensions must be >=0 & <=3  !!!");
            System.out.println(" The dimensions must be >=0 & <=2  !!");
        }


        int[] iDim = new int[4];
        iDim[3] = dim1 == 3 ? dim2 : dim2 == 3 ? dim1 : 3;
        iDim[2] = dim1 == 2 ? dim2 : dim2 == 2 ? dim1 : 2;
        iDim[1] = dim1 == 1 ? dim2 : dim2 == 1 ? dim1 : 1;
        iDim[0] = dim1 == 0 ? dim2 : dim2 == 0 ? dim1 : 0;

        Header header = this.getDataset().getHeader();
        int receiverCounts = header.getNumberParam(DefaultParams.RECEIVER_COUNT).getValue().intValue();

        int[] matrix = new int[4];
        matrix[3] = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_4D).getValue().intValue();
        matrix[2] = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_3D).getValue().intValue();
        matrix[1] = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_2D).getValue().intValue();
        matrix[0] = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();

        if (!header.hasParam(DefaultParams.STATE)) {
            header.putParam(new DefaultParams().getParam(DefaultParams.STATE));
        }

        if (!(header.getParam(DefaultParams.PHASE_0) instanceof ListNumberParam)) {
            header.putParam(new DefaultParams().getParam(DefaultParams.PHASE_0));
            header.putParam(new DefaultParams().getParam(DefaultParams.PHASE_1));
        }

        ExamplePermute.permuteParameters(header.getListNumberParam(DefaultParams.STATE), iDim);
        ExamplePermute.permuteParameters(header.getListNumberParam(DefaultParams.PHASE_0), iDim);
        ExamplePermute.permuteParameters(header.getListNumberParam(DefaultParams.PHASE_1), iDim);

        header.getParam(DefaultParams.MATRIX_DIMENSION_4D).setValue(matrix[iDim[3]]);
        header.getParam(DefaultParams.MATRIX_DIMENSION_3D).setValue(matrix[iDim[2]]);
        header.getParam(DefaultParams.MATRIX_DIMENSION_2D).setValue(matrix[iDim[1]]);
        header.getParam(DefaultParams.MATRIX_DIMENSION_1D).setValue(matrix[iDim[0]]);


        for (int rec = 0; rec < receiverCounts; rec++) {
            float[][][][][] permutedData = permuteData(iDim,
                    this.getDataset().getData(rec).getRealPart(),
                    this.getDataset().getData(rec).getImaginaryPart());
            this.getDataset().setData(permutedData[0], permutedData[1], rec);
        }
    }

    /**
     * Transpose the dimensional parameters.
     *
     * @param dimensionalParam Dimensional parameters.
     * @param iDim             indice of permutation.
     */
    private static void permuteParameters(ListNumberParam dimensionalParam, int[] iDim) {

        Number[] oldValue = new Number[4];
        oldValue[iDim[0]] = dimensionalParam.getValueAt(0, 0);
        oldValue[iDim[1]] = dimensionalParam.getValueAt(1, 0);
        oldValue[iDim[2]] = dimensionalParam.getValueAt(2, 0);
        oldValue[iDim[3]] = dimensionalParam.getValueAt(3, 0);

        dimensionalParam.setValueAt(3, oldValue[3]);
        dimensionalParam.setValueAt(2, oldValue[2]);
        dimensionalParam.setValueAt(1, oldValue[1]);
        dimensionalParam.setValueAt(0, oldValue[0]);
    }

    /**
     * Transpose the dimensional parameters.
     *
     * @param iDim     indice of permutation.
     * @param realData real data
     * @param imagData imaginary data.
     */
    public static float[][][][][] permuteData(int[] iDim, float[][][][] realData, float[][][][] imagData) {

        int[] matrix = new int[4];
        matrix[3] = realData.length;
        matrix[2] = realData[0].length;
        matrix[1] = realData[0][0].length;
        matrix[0] = realData[0][0][0].length;


        float[][][][] newDataReal = new float[matrix[iDim[3]]][matrix[iDim[2]]][matrix[iDim[1]]][matrix[iDim[0]]];
        float[][][][] newDataImag = new float[matrix[iDim[3]]][matrix[iDim[2]]][matrix[iDim[1]]][matrix[iDim[0]]];

        int[] i = new int[4];
        for (i[3] = 0; i[3] < matrix[3]; ++i[3]) {
            for (i[2] = 0; i[2] < matrix[2]; ++i[2]) {
                for (i[1] = 0; i[1] < matrix[1]; ++i[1]) {
                    for (i[0] = 0; i[0] < matrix[0]; ++i[0]) {
                        newDataReal[i[iDim[3]]][i[iDim[2]]][i[iDim[1]]][i[iDim[0]]] = realData[i[3]][i[2]][i[1]][i[0]];
                        newDataImag[i[iDim[3]]][i[iDim[2]]][i[iDim[1]]][i[iDim[0]]] = imagData[i[3]][i[2]][i[1]][i[0]];
                    }
                }
            }
        }
        return new float[][][][][]{newDataReal, newDataImag};
    }
}
