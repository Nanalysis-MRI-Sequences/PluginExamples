package rs2d.setup;

import rs2d.commons.log.Log;
import rs2d.commons.xml.XmlSerializer;
import rs2d.spinlab.application.Application;
import rs2d.spinlab.data.DataSet;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.io.DatasetIO;
import rs2d.spinlab.distrib.Distrib;
import rs2d.spinlab.plugins.setup.SetUpPluginAbstract;
import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.exception.ParamsExceptionSolver;
import rs2d.spinlab.tools.param.exception.UncompatibleParam;
import rs2d.spinlab.tools.utility.Step;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class RunWithVariableParameter extends SetUpPluginAbstract {
    private TextParam parameterName = new TextParam("UserParameter Name",
            DefaultParams.RECEIVER_GAIN.name(), "Parameter to be tested (single value parameter)");

    private ListNumberParam parameterValues = new ListNumberParam("Values", Collections.emptyList()
            , NumberEnum.Double, "Values of the parameter to be tested");


    //
    // Construction
    //
    public RunWithVariableParameter() {
        super("RunWithVariableParameter", "repeat acquisition by changing a UserParameter value ( only NumberParam are accepted)");
    }

    @Override
    public Param[] getParam() {
        MriDefaultParams defaultParams = new MriDefaultParams();
        List<String> suggestedValues = defaultParams.values().stream()
                .map(ParamDescription::name)
                .filter(description -> defaultParams.getParam(description) instanceof NumberParam)
                .collect(Collectors.toList());
        System.out.println(userParams);
        parameterName.setSuggestedValues(suggestedValues);

        return new Param[]{parameterName, parameterValues};
    }

    @Override
    public Param[] getResult() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Get the high resolution data before a dynamic acquisition";
    }

    @Override
    public Param[] execute(Param... params) throws Exception {
        System.out.println(" ");
        System.out.println("  -------");
        System.out.println(" Setup Start");

        checkParameters(params);
        String parameterName = ((TextParam) params[0]).getValue();
        List<Number> parameterValues = ((ListNumberParam) params[1]).getValue();

        int nbAquisition = parameterValues.size();
        ParamLibrary library = serie.getApplication().getUserParams().clone();
        if (!library.contains(parameterName)) {
            serie.setCurrentStep(Step.Error);
            Log.error(getClass(),
                    "The parameter named %s  is not present!", parameterName);
            System.out.println(" The parameter named " + parameterName + " is not present! ");
            return null;
        } else if (!((library.getParam(parameterName).getType().compareTo("NumberParam") == 0) || (library.getParam(parameterName).getType().compareTo("listNumberParam") == 0))) {
            System.out.println(parameterName + "  - " + library.getParam(parameterName).getType());
            serie.setCurrentStep(Step.Error);
            Log.error(getClass(),
                    "The parameter named %s  is not a single value parameter!", parameterName);
            System.out.println(" The parameter named " + parameterName + " is not a single value parameter! ");
            return null;
//            throw new ParameterNotFoundException(String.format(
//                    "The parameter named %s  is not a single value parameter!", parameterName));
        }


        boolean paramTypeNumber;
        paramTypeNumber = (library.getParam(parameterName).getType().compareTo("NumberParam") == 0);

        NumberEnum libUPEnum;
        if (paramTypeNumber) {
            libUPEnum = library.getNumberParam(parameterName).getNumberEnum();
        } else {
            libUPEnum = library.getListNumberParam(parameterName).getNumberEnum();
        }


        int size1D = library.getNumberParam(NmrDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D).intValue();
        int size2D = library.getNumberParam(NmrDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D).intValue();
        int size3D = library.getNumberParam(NmrDefaultParams.ACQUISITION_MATRIX_DIMENSION_3D).intValue();

        int size4DAcqui = library.getNumberParam(NmrDefaultParams.ACQUISITION_MATRIX_DIMENSION_4D).intValue();
        int size4D = size4DAcqui * nbAquisition;
        int sizeRec = library.getNumberParam(NmrDefaultParams.RECEIVER_COUNT).intValue();
        ModalityEnum modalityMRI = library.getTextParam(NmrDefaultParams.MODALITY).getValue().compareToIgnoreCase(ModalityEnum.MRI.name()) == 0 ? ModalityEnum.MRI : ModalityEnum.NMR;

        DataSetInterface signalDataset = new DataSet(size1D, size2D, size3D, size4D, sizeRec, modalityMRI);


        for (int iter = 0; iter < nbAquisition; iter++) {
            // change the parameter
            Application ApplicationAcqu = serie.getApplication().clone();
            if (paramTypeNumber) {
                ApplicationAcqu.getUserParams().getNumberParam(parameterName).setValue(parameterValues.get(iter).doubleValue());
            } else {
                ApplicationAcqu.getUserParams().getListNumberParam(parameterName).getValue().clear();
                ApplicationAcqu.getUserParams().getListNumberParam(parameterName).getValue().add(parameterValues.get(iter).doubleValue());
            }
            // run acquisition

            System.out.println("");
            System.out.println("iter " + iter + "   - " + parameterName + "  = " + parameterValues.get(iter).doubleValue());
            System.out.println("");
            //      System.out.println(Distrib.getInstance().getAcquisitionManager().getSequence().getUserParams());


            setSequence(ApplicationAcqu.getUserParams());
            Distrib.getInstance().startSequencer(true);

//            AcquisitionUtil.runApplication(ApplicationAcqu, ApplicationAcqu.getUserParams());
            DataSetInterface dataset = Distrib.getInstance().getDataset();

            // save the data in signalDataset

            for (int rec = 0; rec < sizeRec; rec++) {
                for (int l = 0; l < size4DAcqui; l++) {
                    int lPos = iter * size4DAcqui + l;
                    for (int k = 0; k < size3D; k++) {
                        for (int j = 0; j < size2D; j++) {
                            for (int i = 0; i < size1D; i++) {

//                                System.out.print("  copy " + rec +" "+ l +" "+ k +" "+ j + " "+i );
                                float newDataReal = dataset.getData(rec).getRealElement(i, j, k, l);
                                float newDataImag = dataset.getData(rec).getImaginaryElement(i, j, k, l);
                                signalDataset.getData(rec).setData(newDataReal, newDataImag, i, j, k, lPos);

                            }
                        }
                    }
                }
            }
            // save header
            String headerName = "header";
            headerName += iter + ".xml";
            File headerFilelocation = new File(serie.getOutputDirectory(), headerName);
            (new XmlSerializer()).serialize(ApplicationAcqu.getUserParams(), headerFilelocation);
//            (new XmlSerializer()).serialize(signalDataset.getHeader(), headerFilelocation); all with default parameter


        }

        // change size and SETUP_PARAMETER_NAME in the header of signalDataset
        library.getNumberParam(NmrDefaultParams.ACQUISITION_MATRIX_DIMENSION_4D).setValue(size4D);
        TextParam setupParameterName = new TextParam("SETUP_PARAMETER_NAME",
                parameterName, "Parameter that did vary");
        ListNumberParam setupParameterValues = new ListNumberParam("SETUP_PARAMETER_VALUES",
                parameterValues, libUPEnum, "Values of the parameter that did vary");

        library.addParam(setupParameterName);
        library.addParam(setupParameterValues);

        //add MATRIX_DIMENSION_1D
        //System.out.println("   MATRIX_DIMENSION_1D  " + library.getNumberParam(NmrDefaultParams.MATRIX_DIMENSION_1D).getValue());
        NumberParam matrix1D = (NumberParam) new DefaultParams().getParam(DefaultParams.MATRIX_DIMENSION_1D);
        matrix1D.setValue(size1D);
        library.addParam(setupParameterName);


        signalDataset.getHeader().putParams(library.getParamsList());

        // add variationParams4D to data set
        ListNumberParam list = new ListNumberParam(parameterName, parameterValues, libUPEnum);
        Map<String, ListNumberParam> variationParams4D = new TreeMap<>(String::compareTo);
        variationParams4D.put(parameterName, list);
        signalDataset.getHeader().setVariationParams4D(variationParams4D);

        // save data
        try {
            DatasetIO.saveDataset(signalDataset, serie.getOutputDirectory());
            File setupOutputDir = new File(serie.getOutputDirectory(), "Setup");
            DatasetIO.saveDataset(signalDataset, setupOutputDir);
        } catch (IOException e) {
            Log.error(getClass(), "Unable to write the calibration dataset.");
            throw e;
        }

        serie.setCurrentStep(Step.ExportDone);
        System.out.println(" ");
        System.out.println(" Setup Done");
        return null;
    }


    /**
     * Calls Distrib.setSequence recursively to solve unreach/uncompatible params
     */
    private void setSequence(ParamLibrary paramLibrary) throws Exception {
        //le bout de code qui sera exécuté
        Callable<Void> callable = () -> {
            Distrib.getInstance().setSequence(sequenceGeneratorName, paramLibrary, null, true);
            return null;
        };

        //appelle le callable récursivement, en changeant les userParams si nécessaire
        ParamsExceptionSolver<Void> solver = new ParamsExceptionSolver<>(callable);
        solver.execute(paramLibrary);

        //affiche les params modifiés
        for (UncompatibleParam param : solver.getChangedParameters()) {
            Log.debug(getClass(), "changed param: " + param);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
