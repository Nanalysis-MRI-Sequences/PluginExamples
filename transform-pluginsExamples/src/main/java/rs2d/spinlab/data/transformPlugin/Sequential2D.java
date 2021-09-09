package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

/**
 * Transformation for sequential acquisition.
 *
 * @author jb
 * @version 1.0, Created on 30 mars 2010
 * @since 1.0
 */
public class Sequential2D extends TransformPluginAbstract {

    private static final String DESCRIPTION = "---------->----------\n"
            + "---------->----------\n"
            + "---------->----------\n"
            + "........>..........\n"
            + "........>..........\n"
            + "........>..........";
    private final static int JUMP_SIZE = 2;
    private static final float VERSION = 1.0f;
    private int etl;
    private int matrix1D;
//    private int matrix2D;
    private int matrix3D;
    private int nbshoot;
    private boolean multiplanar;

    /**
     * The default constructor.
     */
    public Sequential2D() {
        super();

    }

    public void setParameters(List<Param> params) throws UncompatibleParamsException {
        matrix1D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name())).getValue().intValue();
//        matrix2D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name())).getValue().intValue();
        matrix3D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name())).getValue().intValue();
        Param param = Param.getParamFromName(params, MriDefaultParams.ECHO_TRAIN_LENGTH.name());
        if (param != null) {
            etl = ((NumberParam) param).getValue().intValue();
        } else {
            etl = 1;
        }

        param = Param.getParamFromName(params, MriDefaultParams.NUMBER_OF_SHOOT_3D.name());
        if (param != null) {
            nbshoot = ((NumberParam) param).getValue().intValue();
        } else {
            nbshoot = 1;
        }

        param = Param.getParamFromName(params, MriDefaultParams.MULTI_PLANAR_EXCITATION.name());
        if (param != null) {
            multiplanar = ((BooleanParam) param).getValue();
        } else {
            multiplanar = false;
        }
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
        if (multiplanar) {
            for (int i = 0; i < dataReal.length; i++) {
                int[] transf = this.transf(i, j, k, l);
                this.dataset.getData(receiver).setData(dataReal[i], dataImaginary[i], transf[0], transf[1], transf[2], transf[3]);
            }
            return transf(0, j, k, l);
        } else {
            // Check the data size.
            if (dataReal.length >= (etl * matrix1D) && dataImaginary.length >= (etl * matrix1D)) {
                // Copy all data in the dataset.
                for (int i = 0; i < etl; i++) {
                    System.arraycopy(dataReal, i * matrix1D, dataset.getData(receiver).getRealPart()[l][k][j * etl + i], 0, matrix1D);
                    System.arraycopy(dataImaginary, i * matrix1D, dataset.getData(receiver).getImaginaryPart()[l][k][j * etl + i], 0, matrix1D);
                }
            } else {
                // If the size is not fitting : keep the maximum of data.
                for (int i = 0; i < (dataReal.length / matrix1D); i++) {
                    System.arraycopy(dataReal, i * matrix1D, dataset.getData(receiver).getRealPart()[l][k][j * etl + i], 0, matrix1D);
                    System.arraycopy(dataImaginary, i * matrix1D, dataset.getData(receiver).getImaginaryPart()[l][k][j * etl + i], 0, matrix1D);
                }
            }
            return transf(0, j, k, l);
        }
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
        if (multiplanar) {
            int i = scan1D % matrix1D;
            int index3d = (scan1D / (etl * matrix1D));
            int k;
            if (nbshoot == 1) {
                int index3DJumped = index3d * JUMP_SIZE;
                int lastPut = (matrix3D - 1) / JUMP_SIZE;
                k = index3DJumped < matrix3D ? index3DJumped : 1 + (JUMP_SIZE * (index3d - 1 - lastPut));
            } else {
                k = index3d * nbshoot + scan3D;
            }
            int j = (scan1D / matrix1D) % etl + scan2D * etl;
            return new int[]{i, j, k, scan4D};
        } else {
            int i = scan1D % matrix1D;
            int j = (scan1D / matrix1D) + scan2D * etl;
            return new int[]{i, j, scan3D, scan4D};
        }
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
        int scan2D = j / etl;
        if (multiplanar) {
            int scan1D;
            int scan3D;
            if (nbshoot == 1) {
                int transSlicePos = k / JUMP_SIZE + (k % 2 != 0 ? matrix3D / 2 + (matrix3D % 2 != 0 ? 1 : 0) : 0);
                scan1D = i + matrix1D * (j % etl) + transSlicePos * matrix1D * etl;
                scan3D = 0;
            } else {
                scan3D = k % nbshoot;
                scan1D = i + (k / nbshoot) * matrix1D * etl + (j % etl) * matrix1D;
            }
            return new int[]{scan1D, scan2D, scan3D, l};
        } else {
            int scan1D = (j % etl) * matrix1D + i;
            return new int[]{scan1D, scan2D, k, l};
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getName() {
        return "Sequential2D";
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
        test(3, 4, 4, 2, 1, 1, true);
        test(3, 4, 5, 2, 2, 1, true);
        test(3, 4, 5, 2, 4, 1, true);

        test(3, 4, 3, 2, 1, 3, true);
        test(3, 4, 9, 2, 2, 3, true);
        test(3, 4, 4, 2, 2, 2, true);

        test(3, 4, 2, 1, 1, 1, false);
        test(3, 4, 2, 1, 2, 1, false);
        test(3, 4, 2, 1, 4, 1, false);
    }

    private static void test(int mx1D, int mx2D, int mx3D, int mx4D, int netl, int nshoot, boolean multiplanar) {
        int acqu1D = mx1D * (multiplanar ? mx3D / nshoot : 1) * netl;
        int acqu2D = mx2D / netl;
        int acqu3D = multiplanar ? nshoot : mx3D;
        int acqu4D = mx4D;

        System.out.println("Sequential2D " + " Multiplanar " + multiplanar + " " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D + " Etl " + netl + " Nshoot " + nshoot);
        TransformUtility.test(new Sequential2D(), mx1D, mx2D, mx3D, mx4D, netl, nshoot, multiplanar, acqu1D, acqu2D, acqu3D, acqu4D);
    }
}
