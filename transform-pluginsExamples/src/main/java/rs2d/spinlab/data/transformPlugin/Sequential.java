package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.NumberParam;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

/**
 * Transformation for sequential acquisition.
 *
 * @author jb
 * @version 1.0, Created on 30 mars 2010
 * @since 1.0
 */
public class Sequential extends TransformPluginAbstract {
    private static final String DESCRIPTION = "--1D--2D--3D--4D--\n";
    private static final String NAME = "Sequential";

    private int matrix1D;
 
    /**
     * The default constructor.
     */
    public Sequential() {
        super();

    }

   
    @Override
    public void setParameters(List<Param> params) throws UncompatibleParamsException {
        matrix1D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name())).getValue().intValue();
    }

    /**
     * Put new data into the dataset.
     *
     * @param dataReal data to add
     * @param dataImaginary data to add
     * @param j
     * @param k
     * @param l
     * @param receiver the number of the receivers
     * @return kpace coordonate of the first data.
     */
    @Override
    public int[] putData(float[] dataReal, float[] dataImaginary, int j, int k, int l, int receiver) {
        for (int i = 0; i < Math.max(dataReal.length,matrix1D); i++) {
            int[] transf = this.transf(i, j, k, l);
            this.dataset.getData(receiver).setData(dataReal[i], dataImaginary[i], transf[0], transf[1], transf[2], transf[3]);
        }
        return transf(0, j, k, l);
    }

    /**
     * Transform K space index from the acquisition index.
     *
     * @param scan1D the first acquisition dimension.
     * @param scan2D the second acquisition dimension.
     * @param scan3D the third acquisition dimension.
     * @param scan4D the forth acquisition dimension.
     * @return the index in the Fourier space.
     */
    @Override
    public int[] transf(int scan1D, int scan2D, int scan3D, int scan4D) {
        return new int[]{scan1D, scan2D, scan3D, scan4D};
    }

    /**
     * Transform the acquisition index from K space index.
     *
     * @param i the first acquisition dimension.
     * @param j the second acquisition dimension.
     * @param k the third acquisition dimension.
     * @param l the forth acquisition dimension.
     * @return the index in the acquisition space.
     */
    @Override
    public int[] invTransf(int i, int j, int k, int l) {
        return new int[]{i, j, k, l};
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getSignificantEcho() {
        return 1;
    }

    @Override
    public int getScanOrder() {
        return 2;
    }

    public static void main(String args[]) {
        test(3, 4, 4, 2);
        test(3, 4, 5, 2);
        test(3, 4, 5, 2);

        test(3, 4, 3, 2);
        test(3, 4, 9, 2);
        test(3, 4, 4, 2);
    }

    private static void test(int mx1D, int mx2D, int mx3D, int mx4D) {
        int acqu1D = mx1D ;
        int acqu2D = mx2D;
        int acqu3D = mx3D;
        int acqu4D = mx4D;

        System.out.println("Sequential2D " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D );
        TransformUtility.test(new Sequential(), mx1D, mx2D, mx3D, mx4D, 1, mx3D, false, acqu1D, acqu2D, acqu3D, acqu4D);
    }
}
