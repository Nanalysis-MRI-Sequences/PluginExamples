///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package rs2d.spinlab.data.transformPlugin;
//
//import org.junit.Test;
//import rs2d.spinlab.data.DataSet;
//import rs2d.spinlab.data.DataSetInterface;
//import rs2d.spinlab.tools.param.ModalityEnum;
//import rs2d.spinlab.tools.param.MriDefaultParams;
//import rs2d.spinlab.tools.param.Param;
//
///**
// *
// * @author rs2d
// */
//public class CenteredTransformTest {
//
//    public CenteredTransformTest() {
//    }
//
//    @Test
//    public final void testInitiateMaps() {
//        System.out.println("initiateMaps");
//
////        final Header header1 = new Header();
////        final MriDefaultParams mriDefaultParams = new MriDefaultParams();
////        List<Param> requieredParam = mriDefaultParams.getRequiredParam();
////        for (final Param param : requieredParam) {
////            header1.putParam(param);
////        }
////        requieredParam = mriDefaultParams.getRequiredParam();
////        for (final Param param : requieredParam) {
////            header1.putParam(param);
////        }
////        this.dataSet.setHeader(header1);
////        header1.getParam(DefaultParams.MATRIX_DIMENSION_1D.name()).setValue(Size1D);
////        header1.getParam(DefaultParams.MATRIX_DIMENSION_2D.name()).setValue(Size2D);
////        header1.getParam(MriDefaultParams.MATRIX_DIMENSION_3D.name()).setValue(Size3D);
////        header1.getParam(MriDefaultParams.MATRIX_DIMENSION_4D.name()).setValue(Size4D);
//
//
//        dataSet = new DataSet(Size1D, Size2D*4, Size3D, Size4D, 1, ModalityEnum.MRI);
//
//        final MriDefaultParams mriDefaultParams = new MriDefaultParams();
//        Param param = mriDefaultParams.getParam(MriDefaultParams.ECHO_TRAIN_LENGTH.name());
//        param.setValue(4);
//        dataSet.getHeader().putParam(param);
//
////        final Param paramETL = mriDefaultParams.getParam(MriDefaultParams.ECHO_TRAIN_LENGTH.name());
////
////        paramETL.setValue(2);
////        header1.putParam(paramETL);
////        try {
////            this.dataSet.initiateMaps();
////        } catch (Exception ex) {
////            Logger.getLogger(CenteredTransformTest.class.getName()).log(Level.SEVERE, null, ex);
////        }
//    }
//    /**
//     * The current {@link DataSet}.
//     */
//    private DataSetInterface dataSet = new DataSet();
//
//    /**
//     * Test of putData method, of class EPITransform.
//     */
//    @Test
//    public void testPutData() {
//        System.out.println("putData");
//        float[] dataReal = {0f, 1f, 2f, 3f};
//        float[] dataImaginary = {0f, 0f, 2f, 0f};
//        this.testInitiateMaps();
//
//        int j = 0;
//        int k = 0;
//        int l = 0;
////        float data = 10f;
//        int receiver = 0;
//
//        CenteredTransform instance = new CenteredTransform();
//        instance.setDataset(dataSet);
////        for (int i = 0; i < Size2D/2; i++) {
//        instance.putData(dataReal, dataImaginary, 0, k, l, receiver);
//
//        float[] dataReal2 = {4f, 5f, 6f, 7f};
//        instance.putData(dataReal2, dataImaginary, 1, k, l, receiver);
//
////        }
//        soutMatrice(dataSet.getData(receiver).getRealPart()[0][0]);
//        System.out.println("");
//        soutMatrice(dataSet.getData(receiver).getImaginaryPart()[0][0]);
//
////        assertEquals("Test put data", 3, dataSet.getData(receiver).getRealPart()[0][0][2][1], 0.1f);
////        assertEquals("Test put data", 0, dataSet.getData(receiver).getImaginaryPart()[0][0][1][1], 0.1f);
//
//        int[] transf = instance.transf(2, 1, 0, 0);
//        System.out.println("");
//        System.out.print("transf ");
//        for (int i : transf) {
//            System.out.print(i + " ");
//        }
//        int[] itransf = instance.invTransf(transf[0], transf[1], transf[2], transf[3]);
//        System.out.println("");
//        System.out.print("Invers ");
//        for (int i : itransf) {
//            System.out.print(i + " ");
//        }
//    }
//    private int Size1D = 2;
//    private int Size2D = 2;
//    private int Size3D = 6;
//    private int Size4D = 2;
//
//    /**
//     * Test of putData method, of class EPITransform.
//     */
//    @Test
//    public void testAll() {
//        this.testInitiateMaps();
//        CenteredTransform instance = new CenteredTransform();
//        instance.setDataset(dataSet);
//        
//        System.out.println("Test all");
//        for (int j = 0; j < Size2D*4; j++) {
//            for (int i = 0; i < Size1D; i++) {
//                System.out.print(instance.invTransf(i, j, 0, 0)[1] + " ");
//            }
//            System.out.println("\n");
//        }
//    }
//
//    /**
//     * Print the matrix.
//     * @param table the matrix to print
//     */
//    final void soutMatrice(final float[][] table) {
//
//        for (int j = 0; j < table.length; j++) {
//            System.out.println("");
//            for (int i = 0; i < table[0].length; i++) {
//                System.out.print(table[j][i] + "  ");
//            }
//            System.out.println("");
//        }
//    }
//}
