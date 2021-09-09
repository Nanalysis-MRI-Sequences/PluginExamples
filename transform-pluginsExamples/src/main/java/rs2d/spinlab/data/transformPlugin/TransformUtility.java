/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.data.DataSet;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.tools.param.*;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Julien Muller
 */
public class TransformUtility {

//    public static boolean test(TransformPlugin trans, int mx1D, int mx2D, int mx3D, int etl, boolean allInAcqu1D, boolean debug) {
//        int acqu1D = 1;
//        int acqu2D = 1;
//        int acqu3D = 1;
//
//        if (allInAcqu1D) {
//            acqu1D = mx1D * etl * mx3D;
//            acqu2D = mx2D / etl;
//            acqu3D = 1;
//        } else {
//            acqu1D = mx1D * etl;
//            acqu2D = mx2D / etl;
//            acqu3D = mx3D;
//        }
//        HashMap<String, Param> params = new HashMap<String, Param>();
//        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
//                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
//                        mx1D, NumberEnum.Integer));
//        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
//                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
//                        mx2D, NumberEnum.Integer));
//        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
//                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
//                        mx3D, NumberEnum.Integer));
//        params.put(MriDefaultParams.ECHO_TRAIN_LENGTH.name(),
//                new NumberParam(MriDefaultParams.ECHO_TRAIN_LENGTH.name(),
//                        etl, NumberEnum.Integer));
//        Header header = new Header();
//        header.setParams(params);
//        DataSetInterface dataset = new DataSet();
//        dataset.setHeader(header);
//        dataset.matrixToAcquisitionMatrix();
//        trans.setDataset(dataset);
//
//        boolean bug = false;
//        for (int cpt3D = 0; cpt3D < acqu3D && !bug; cpt3D++) {
//            for (int cpt2D = 0; cpt2D < acqu2D && !bug; cpt2D++) {
//                for (int cpt1D = 0; cpt1D < acqu1D && !bug; cpt1D++) {
//
//                    int[] res = trans.transf(cpt1D, cpt2D, cpt3D, 0);
//                    if (debug) {
//                        System.out.println("cpt :      \t" + cpt1D + "\t" + cpt2D + "\t" + cpt2D);
//                        System.out.println("trans to   \t" + res[0] + "\t" + res[1] + "\t" + res[2] + "\t" + res[3]);
//                    }
//                    int[] inv = trans.invTransf(res[0], res[1], res[2], res[3]);
//                    if (debug) {
//                        System.out.println("reverse to \t" + inv[0] + "\t" + inv[1] + "\t" + inv[2] + "\t" + inv[3]);
//                        System.out.println();
//                    }
//
//                    if (inv[0] != cpt1D || inv[1] != cpt2D || inv[2] != cpt3D) {
//                        if (!debug) {
//                            System.err.println("et merde ca marche toujours pas...");
//                            System.err.println("cpt :      \t" + cpt1D + "\t" + cpt2D + "\t" + cpt3D);
//                            System.err.println("trans to   \t" + res[0] + "\t" + res[1] + "\t" + res[2] + "\t" + res[3]);
//                            System.err.println("reverse to \t" + inv[0] + "\t" + inv[1] + "\t" + inv[2]);
//
//                            bug = true;
//                        }
//                    }
//                }
//
//            }
//        }
//        return !bug;
//
//    }
//    public static void main(String args[]) {
//        int mx1D = 256;
//        int mx2D = 128;
//        int mx3D = 32;
//        int etl = 1;
//        boolean allInAcqu1D = false;
//
//        TransformPlugin trans = new Jump3DTransform();
//        boolean succed = TransformUtility.test(trans, mx1D, mx2D, mx3D, etl, allInAcqu1D, false);
//        if (succed == false) {
//            System.err.println("TEST FAILED");
//            //TransformUtility.test(trans, mx1D, mx2D, mx3D, etl, allInAcqu1D, true);
//        } else {
//            System.out.println("TEST OK");
//        }
//
//    }
    static void test(TransformPlugin transformPlugin, int mx1D, int mx2D, int mx3D, int mx4D, int netl, int nshoot, boolean multiplanar, int acqu1D, int acqu2D, int acqu3D, int acqu4D) {
        Map<String, Param> params = new TreeMap<>(String::compareTo);
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
                        mx1D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
                        mx2D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
                        mx3D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_4D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_4D.name(),
                        mx4D, NumberEnum.Integer));
        params.put(MriDefaultParams.NUMBER_OF_SHOOT_3D.name(),
                new NumberParam(MriDefaultParams.NUMBER_OF_SHOOT_3D.name(),
                        nshoot, NumberEnum.Integer));
        params.put(MriDefaultParams.ECHO_TRAIN_LENGTH.name(),
                new NumberParam(MriDefaultParams.ECHO_TRAIN_LENGTH.name(),
                        netl, NumberEnum.Integer));
        params.put(MriDefaultParams.MULTI_PLANAR_EXCITATION.name(),
                new BooleanParam(MriDefaultParams.MULTI_PLANAR_EXCITATION.name(), multiplanar, MriDefaultParams.MULTI_PLANAR_EXCITATION.name()));

        Header header = new Header();
        header.setParams(params);
        DataSetInterface dataset = new DataSet();
        dataset.setHeader(header);

        transformPlugin.setDataset(dataset);

        String[][][][] s = new String[mx4D][mx3D][mx2D][mx1D];

        for (int cpt4D = 0; cpt4D < acqu4D; cpt4D++) {
            for (int cpt3D = 0; cpt3D < acqu3D; cpt3D++) {
                for (int cpt2D = 0; cpt2D < acqu2D; cpt2D++) {
                    for (int cpt1D = 0; cpt1D < acqu1D; cpt1D++) {
                        int[] res = transformPlugin.transf(cpt1D, cpt2D, cpt3D, cpt4D);
                        s[res[3]][res[2]][res[1]][res[0]] = cpt1D + " " + cpt2D + " " + cpt3D + " " + cpt4D;
                    }
                }
            }
        }
        boolean failed = false;
        int i = 0;
        for (int cpt4D = 0; cpt4D < mx4D; cpt4D++) {
            for (int cpt3D = 0; cpt3D < mx3D; cpt3D++) {
                for (int cpt2D = 0; cpt2D < mx2D; cpt2D++) {
                    for (int cpt1D = 0; cpt1D < mx1D; cpt1D++) {
                        int[] res = transformPlugin.invTransf(cpt1D, cpt2D, cpt3D, cpt4D);
                        boolean equals = s[cpt4D][cpt3D][cpt2D][cpt1D].equals(res[0] + " " + res[1] + " " + res[2] + " " + res[3]);
                        i++;
                        if (!equals) {
                            System.out.println("Find " + res[0] + " " + res[1] + " " + res[2] + " " + res[3] + " but should be " + s[cpt4D][cpt3D][cpt2D][cpt1D]);
                            failed = true;
                        }
                    }
                }
            }
        }

        System.out.println("Test done for " + i + " coordinates and " + (!failed ? "Succeed" : "Failed"));
    }

    static void testCardiac(TransformPlugin transformPlugin, int mx1D, int mx2D, int mx3D, int mx4D, int acqu1D, int acqu2D, int acqu3D, int acqu4D, int nbPhase, int nbCycle, boolean type4D) {
        Map<String, Param> params = new TreeMap<>(String::compareTo);
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name(),
                        mx1D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(),
                        mx2D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name(),
                        mx3D, NumberEnum.Integer));
        params.put(DefaultParams.ACQUISITION_MATRIX_DIMENSION_4D.name(),
                new NumberParam(DefaultParams.ACQUISITION_MATRIX_DIMENSION_4D.name(),
                        mx4D, NumberEnum.Integer));
        params.put("CARDIAC_NB_OF_PHASE_PER_CYCLE",
                new NumberParam("CARDIAC_NB_OF_PHASE_PER_CYCLE",
                        nbPhase, NumberEnum.Integer));
        params.put("CARDIAC_NB_OF_CYCLE",
                new NumberParam("CARDIAC_NB_OF_CYCLE",
                        nbCycle, NumberEnum.Integer));
        params.put("CARDIAC_LOOP_OR_NB4D",
                new BooleanParam("CARDIAC_LOOP_OR_NB4D",
                        type4D, ""));

        Header header = new Header();
        header.setParams(params);
        DataSetInterface dataset = new DataSet();
        dataset.setHeader(header);

        transformPlugin.setDataset(dataset);

        String[][][][] s = new String[mx4D][mx3D][mx2D][mx1D];

        for (int cpt4D = 0; cpt4D < acqu4D; cpt4D++) {
            for (int cpt3D = 0; cpt3D < acqu3D; cpt3D++) {
                for (int cpt2D = 0; cpt2D < acqu2D; cpt2D++) {
                    for (int cpt1D = 0; cpt1D < acqu1D; cpt1D++) {
                        int[] res = transformPlugin.transf(cpt1D, cpt2D, cpt3D, cpt4D);
                        s[res[3]][res[2]][res[1]][res[0]] = cpt1D + " " + cpt2D + " " + cpt3D + " " + cpt4D;
                    }
                }
            }
        }
        boolean failed = false;
        int i = 0;
        for (int cpt4D = 0; cpt4D < mx4D; cpt4D++) {
            for (int cpt3D = 0; cpt3D < mx3D; cpt3D++) {
                for (int cpt2D = 0; cpt2D < mx2D; cpt2D++) {
                    for (int cpt1D = 0; cpt1D < mx1D; cpt1D++) {
                        int[] res = transformPlugin.invTransf(cpt1D, cpt2D, cpt3D, cpt4D);
                        boolean equals = s[cpt4D][cpt3D][cpt2D][cpt1D].equals(res[0] + " " + res[1] + " " + res[2] + " " + res[3]);
                        i++;
                        if (!equals) {
                            System.out.println("Find " + res[0] + " " + res[1] + " " + res[2] + " " + res[3] + " but should be " + s[cpt4D][cpt3D][cpt2D][cpt1D]);
                            failed = true;
                        }
                    }
                }
            }
        }

        System.out.println("Test done for " + i + " coordinates and " + (!failed ? "Succeed" : "Failed"));
    }
}
