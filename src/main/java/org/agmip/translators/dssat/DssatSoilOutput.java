package org.agmip.translators.dssat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import static org.agmip.util.MapUtil.*;

/**
 * DSSAT Soil Data I/O API Class
 *
 * @author Meng Zhang
 * @version 1.0
 */
public class DssatSoilOutput extends DssatCommonOutput {

    /**
     * DSSAT Soil Data Output method
     *
     * @param arg0 file output path
     * @param result data holder object
     */
    @Override
    public void writeFile(String arg0, Map result) {

        // Initial variables
//        ArrayList soilSites;                            // Soil site data array
        HashMap soilSite;                       // Data holder for one site of soil data
        ArrayList soilRecords;                          // Soil layer data array
        HashMap soilRecord;                     // Data holder for one layer data
        BufferedWriter bwS;                             // output object
        StringBuilder sbData = new StringBuilder();     // construct the data info in the output
        StringBuilder sbLyrP2 = new StringBuilder();    // output string for second part of layer data
        boolean p2Flg;
        String[] p2Ids = {"slpx", "slpt", "slpo", "caco3", "slal", "slfe", "slmn", "slbs", "slpa", "slpb", "slke", "slmg", "slna", "slsu", "slec", "slca"};
        String layerKey = "soilLayer";  // P.S. the key name might change

        try {

            // Set default value for missing data
            setDefVal();

            soilSite = (HashMap) getObjectOr(result, "soil", new HashMap());
            if (soilSite.isEmpty()) {
                return;
            }
//            decompressData(soilSites);

            // Initial BufferedWriter
            // Get File name
            String soilId = getObjectOr(soilSite, "soil_id", "");
            String fileName;
            if (soilId.equals("")) {
                fileName = "soil.SOL";
            } else {
                try {
                    fileName = soilId.substring(0, 2) + ".SOL";
                } catch (Exception e) {
                    fileName = "soil.SOL";
                }

            }
            arg0 = revisePath(arg0);
            outputFile = new File(arg0 + fileName);
            boolean existFlg = outputFile.exists();
            bwS = new BufferedWriter(new FileWriter(outputFile, existFlg));

            // Output Soil File
            // Description info for output by translator
            if (!existFlg) {
                bwS.write("!This soil file is created by DSSAT translator tool on " + Calendar.getInstance().getTime() + ".\r\n");
            }

            // Titel Section
            if (!existFlg) {
                sbData.append("*SOILS: ").append(getObjectOr((HashMap) soilSite, "sl_notes", defValBlank)).append("\r\n\r\n");
            }
            sbData.append("!The ACE ID is ").append(getValueOr(result, "id", "N/A")).append(".\r\n");
            sbData.append("!This soil data is used for the experiment of ").append(getValueOr(result, "exname", "N/A")).append(".\r\n\r\n");

            // Site Info Section
            sbData.append(String.format("*%1$-10s  %2$-11s %3$-5s %4$5s %5$s\r\n",
                    getObjectOr(soilSite, "soil_id", defValC).toString(),
                    getObjectOr(soilSite, "sl_source", defValC).toString(),
                    getObjectOr(soilSite, "sltx", defValC).toString(),
                    formatNumStr(5, soilSite, "sldp", defValR),
                    getObjectOr(soilSite, "soil_name", defValC).toString()));
            sbData.append("@SITE        COUNTRY          LAT     LONG SCS FAMILY\r\n");
            sbData.append(String.format(" %1$-11s %2$-11s %3$9s%4$8s %5$s\r\n",
                    getObjectOr(soilSite, "sl_loc_3", defValC).toString(),
                    getObjectOr(soilSite, "sl_loc_1", defValC).toString(),
                    formatNumStr(8, soilSite, "soil_lat", defValR), // P.S. Definition changed 9 -> 10 (06/24)
                    formatNumStr(8, soilSite, "soil_long", defValR), // P.S. Definition changed 9 -> 8  (06/24)
                    getObjectOr(soilSite, "classification", defValC).toString()));
            sbData.append("@ SCOM  SALB  SLU1  SLDR  SLRO  SLNF  SLPF  SMHB  SMPX  SMKE\r\n");
            if (getObjectOr(soilSite, "slnf", "").equals("")) {
                sbError.append("! Warning: missing data : [slnf], and will automatically use default value '1'\r\n");
            }
            if (getObjectOr(soilSite, "slpf", "").equals("")) {
                sbError.append("! Warning: missing data : [slpf], and will automatically use default value '0.92'\r\n");
            }
            sbData.append(String.format(" %1$5s %2$5s %3$5s %4$5s %5$5s %6$5s %7$5s %8$-5s %9$-5s %10$-5s\r\n",
                    getObjectOr(soilSite, "scom", defValC).toString(),
                    formatNumStr(5, soilSite, "salb", defValR),
                    formatNumStr(5, soilSite, "slu1", defValR),
                    formatNumStr(5, soilSite, "sldr", defValR),
                    formatNumStr(5, soilSite, "slro", defValR),
                    formatNumStr(5, soilSite, "slnf", "1"), // P.S. Set default value as '1'
                    formatNumStr(5, soilSite, "slpf", "0.92"), // P.S. Set default value as '0.92'
                    getObjectOr(soilSite, "smhb", defValC).toString(),
                    getObjectOr(soilSite, "smpx", defValC).toString(),
                    getObjectOr(soilSite, "smke", defValC).toString()));

            // Soil Layer data section
            soilRecords = (ArrayList) getObjectOr(soilSite, layerKey, new ArrayList());

            // part one
            sbData.append("@  SLB  SLMH  SLLL  SDUL  SSAT  SRGF  SSKS  SBDM  SLOC  SLCL  SLSI  SLCF  SLNI  SLHW  SLHB  SCEC  SADC\r\n");
            // part two
            // Get first site record
            HashMap fstRecord = new HashMap();
            if (!soilRecords.isEmpty()) {
                fstRecord = (HashMap) soilRecords.get(0);
            }

            // Check if there is 2nd part of layer data for output
            p2Flg = false;
            for (int j = 0; j < p2Ids.length; j++) {
                if (!getObjectOr(fstRecord, p2Ids[j], "").toString().equals("")) {
                    p2Flg = true;
                    break;
                }
            }
            if (p2Flg) {
                sbLyrP2.append("@  SLB  SLPX  SLPT  SLPO CACO3  SLAL  SLFE  SLMN  SLBS  SLPA  SLPB  SLKE  SLMG  SLNA  SLSU  SLEC  SLCA\r\n");
            }

            // Loop for laryer data
            for (int j = 0; j < soilRecords.size(); j++) {

                soilRecord = (HashMap) soilRecords.get(j);
                // part one
                sbData.append(String.format(" %1$5s %2$5s %3$5s %4$5s %5$5s %6$5s %7$5s %8$5s %9$5s %10$5s %11$5s %12$5s %13$5s %14$5s %15$5s %16$5s %17$5s\r\n",
                        formatNumStr(5, soilRecord, "sllb", defValR),
                        getObjectOr(soilRecord, "slmh", defValC).toString(),
                        formatNumStr(5, soilRecord, "slll", defValR),
                        formatNumStr(5, soilRecord, "sldul", defValR),
                        formatNumStr(5, soilRecord, "slsat", defValR),
                        formatNumStr(5, soilRecord, "slrgf", defValR),
                        formatNumStr(5, soilRecord, "sksat", defValR),
                        formatNumStr(5, soilRecord, "slbdm", defValR),
                        formatNumStr(5, soilRecord, "sloc", defValR),
                        formatNumStr(5, soilRecord, "slcly", defValR),
                        formatNumStr(5, soilRecord, "slsil", defValR),
                        formatNumStr(5, soilRecord, "slcf", defValR),
                        formatNumStr(5, soilRecord, "slni", defValR),
                        formatNumStr(5, soilRecord, "slphw", defValR),
                        formatNumStr(5, soilRecord, "slphb", defValR),
                        formatNumStr(5, soilRecord, "slcec", defValR),
                        formatNumStr(5, soilRecord, "sadc", defValR)));

                // part two
                if (p2Flg) {
                    sbLyrP2.append(String.format(" %1$5s %2$5s %3$5s %4$5s %5$5s %6$5s %7$5s %8$5s %9$5s %10$5s %11$5s %12$5s %13$5s %14$5s %15$5s %16$5s %17$5s\r\n",
                            formatNumStr(5, soilRecord, "sllb", defValR),
                            formatNumStr(5, soilRecord, "slpx", defValR),
                            formatNumStr(5, soilRecord, "slpt", defValR),
                            formatNumStr(5, soilRecord, "slpo", defValR),
                            formatNumStr(5, soilRecord, "caco3", defValR), // P.S. Different with document (DSSAT vol2.pdf)
                            formatNumStr(5, soilRecord, "slal", defValR),
                            formatNumStr(5, soilRecord, "slfe", defValR),
                            formatNumStr(5, soilRecord, "slmn", defValR),
                            formatNumStr(5, soilRecord, "slbs", defValR),
                            formatNumStr(5, soilRecord, "slpa", defValR),
                            formatNumStr(5, soilRecord, "slpb", defValR),
                            formatNumStr(5, soilRecord, "slke", defValR),
                            formatNumStr(5, soilRecord, "slmg", defValR),
                            formatNumStr(5, soilRecord, "slna", defValR),
                            formatNumStr(5, soilRecord, "slsu", defValR),
                            formatNumStr(5, soilRecord, "slec", defValR),
                            formatNumStr(5, soilRecord, "slca", defValR)));
                }
            }

            // Add part two
            if (p2Flg) {
                sbData.append(sbLyrP2.toString()).append("\r\n");
//                sbLyrP2 = new StringBuilder();
            } else {
                sbData.append("\r\n");
            }

            // Output finish
            bwS.write(sbError.toString());
            bwS.write(sbData.toString());
            bwS.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
