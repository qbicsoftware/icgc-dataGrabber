import models.barcode.BarcodeProducerObject;
import models.donor.Donor;
import models.qwizard.AbstractQWizardRow;
import models.qwizard.QWizardRowFactory;
import models.qwizard.RowTypes;
import models.qwizard.WizardHeader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by fillinger on 11/23/15.
 * The allmighty parser class, converting a donor object fetched from ICGC to
 * the QWizards tsv file format.
 */
public class DonorToQWizardParser {

    /**
     * Parses a list of Donor objects into QWizard tsv file-format.
     * @param bufferedWriter A buffered writer object
     * @param donorList a list containing donor objects
     */
    public static void parse(BufferedWriter bufferedWriter, List<Donor> donorList, final String SPACE)
            throws IOException {

        // The barcode production factory
        QWizardRowFactory qWizardRowFactory = new QWizardRowFactory();

        int donorCounter = 1;
        // write the file header

        bufferedWriter.write(WizardHeader.getHeader());
        bufferedWriter.newLine();

        BarcodeProducerObject extraBarcoder = new BarcodeProducerObject("QICGC", 1, 'W');
        /*
        Iterate deeply through the donor object and parse it
         */
        for (Donor donor : donorList){
            // Get a Entity row
            AbstractQWizardRow entity = qWizardRowFactory.getWizardRow(RowTypes.ENTITY);
            entity.setEntityNumber(donorCounter);
            entity.setSecondaryName(donor.getDonorID());
            entity.setSpace(SPACE);
            // write row
            bufferedWriter.write(entity.toString());
            bufferedWriter.newLine();
            /*
            Iterate through specimen
             */
            for (Donor.Specimen specimen : donor.getSpecimenList()){
                // Get a Biological Sample row
                AbstractQWizardRow bioSample = qWizardRowFactory.getWizardRow(RowTypes.BIO_SAMPLE);
                bioSample.setEntityNumber();
                bioSample.setSpace(SPACE);
                bioSample.setSecondaryName(specimen.getSpecimenID());
                bioSample.setParent(entity.getEntity());
                bioSample.setTissueDetailed(specimen.getSpecimenType());

                if(specimen.getSpecimenType().contains("tumour")){
                    bioSample.setPrimaryTissue("TUMOR_TISSUE_UNSPECIFIED");
                } else if(specimen.getSpecimenType().contains("blood")){
                    bioSample.setPrimaryTissue("BLOOD_PLASMA");
                } else if(specimen.getSpecimenType().contains("EBV")) {
                    bioSample.setPrimaryTissue("CELL_LINE");
                }

                // write row
                bufferedWriter.write(bioSample.toString());
                bufferedWriter.newLine();

                // TODO: can be removed, after linus icgc project is finished
                if(bioSample.getPrimaryTissue().contains("CELL_LINE")){
                    continue;
                }
                /*
                Iterate through samples
                 */
                for (Donor.Sample sample: specimen.getSampleList()){
                    // Get the Test Sample and Single Sample Run rows
                    AbstractQWizardRow testSample = qWizardRowFactory.getWizardRow(RowTypes.TEST_SAMPLE);
                    AbstractQWizardRow singleSampleRun = qWizardRowFactory.getWizardRow(RowTypes.SINGLE_SAMPLE_RUN);
                    // Set the content for test sample
                    //testSample.setEntityNumber();
                    testSample.setSpace(SPACE);
                    testSample.setSecondaryName(sample.getSampleID());
                    testSample.setParent(bioSample.getEntity());
                    testSample.setQSampleType(sample.getLibraryTypes());

                    // TODO: this if-statement can be removed, after Linus' project is fixed and complete
                    if (bioSample.getPrimaryTissue().equals("BLOOD_PLASMA")){
                        testSample.setIdentifier(extraBarcoder.getBarcode());
                        singleSampleRun.setIdentifier(extraBarcoder.getBarcode());
                        singleSampleRun.setSpace(SPACE);
                        singleSampleRun.setSecondaryName(sample.getAnalysedID());
                        singleSampleRun.setParent(testSample.getEntity());

                        // write both rows
                        bufferedWriter.write(testSample.toString());
                        bufferedWriter.newLine();
                        bufferedWriter.write(singleSampleRun.toString());
                        bufferedWriter.newLine();
                        continue;
                    } else{
                        testSample.setEntityNumber();
                    }

                    if(!testSample.getQSampleType().contains("NOLIB")){
                        // Set the content for single sample run
                        singleSampleRun.setEntityNumber();
                        singleSampleRun.setSpace(SPACE);
                        singleSampleRun.setSecondaryName(sample.getAnalysedID());
                        singleSampleRun.setParent(testSample.getEntity());

                        // write both rows
                        bufferedWriter.write(testSample.toString());
                        bufferedWriter.newLine();
                        bufferedWriter.write(singleSampleRun.toString());
                        bufferedWriter.newLine();
                    }
                }

            }
            donorCounter++;
        }
    }


}