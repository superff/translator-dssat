package org.agmip.translators.dssat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import static org.agmip.util.MapUtil.*;

/**
 *
 * @author Meng Zhang
 */
public class DssatWthFileHelper {
    
    private HashSet<String> names = new HashSet();
    private HashMap<String, String> hashToName = new HashMap();
    private int defInsiName = 0xAAAA;
    
    /**
     * Generate the weather file name for auto-generating (extend name not included)
     *
     * @param wthData weather data holder
     * @return
     */
    public String createWthFileName(Map wthData) {
        
        String hash = getObjectOr(wthData, "wst_id", "").toString();
        if (hash.equals("")) {
            return "";
        }
        
        if (hashToName.containsKey(hash)) {
            return hashToName.get(hash);
        } else {
            String insiName = getWthInsiCodeOr(wthData);
            String yearDur = getWthYearDuration(wthData);
            while (names.contains(insiName + yearDur)) {
                insiName = getNextDefName();
            }
            names.add(insiName + yearDur);
            hashToName.put(hash, insiName + yearDur);
            return insiName + yearDur;
        }
    }
    
    /**
     * Get the 4-bit institute code for weather file name, if not available from data holder, then use default code
     * 
     * @param wthData   Weather data holder
     * @return 
     */
    private String getWthInsiCodeOr(Map wthData) {
        String insiName = getWthInsiCode(wthData);
        if (insiName.equals("")) {
            return getNextDefName();
        } else {
            return insiName;
        }
    }
    /**
     * Generate the institute code
     * 
     * @return 
     */
    private String getNextDefName() {
        return Integer.toHexString(defInsiName++).toUpperCase();
    }
    
    

    /**
     * Get the 4-bit institute code for weather file nam
     *
     * @param wthFile weather data holder
     * @return
     */
    public static String getWthInsiCode(Map wthData) {
        String wst_id = getValueOr(wthData, "wst_id", "");
        if (wst_id.length() == 4) {
            return wst_id;
        }
        
        String agmipFileHack = getValueOr(wthData, "wst_name", "");
        if (agmipFileHack.length() >= 4) {
            return agmipFileHack.substring(0, 4);
        } else {
            return "";
        }
    }
    
    /**
     * Get the last 2-bit year number and 2-bit of the duration for weather file name
     *
     * @param wthFile weather data holder
     * @return
     */
    public static String getWthYearDuration(Map wthData) {
        String yearDur = "";
        ArrayList<Map> wthRecords = (ArrayList) getObjectOr(wthData, "dailyWeather", new ArrayList());
        if (!wthRecords.isEmpty()) {
            // Get the year of starting date and end date
            String startYear = getValueOr((wthRecords.get(0)), "w_date", "    ").substring(2, 4).trim();
            String endYear = getValueOr((wthRecords.get(wthRecords.size() - 1)), "w_date", "    ").substring(2, 4).trim();
            // If not available, do not show year and duration in the file name
            if (!startYear.equals("") && !endYear.equals("")) {
                yearDur += startYear;
                try {
                    int iStartYear = Integer.parseInt(startYear);
                    int iEndYear = Integer.parseInt(endYear);
                    iStartYear += iStartYear <= 15 ? 2000 : 1900; // P.S. 2015 is the cross year for the current version
                    iEndYear += iEndYear <= 15 ? 2000 : 1900; // P.S. 2015 is the cross year for the current version
                    int duration = iEndYear - iStartYear + 1;
                    // P.S. Currently the system only support the maximum of 99 years for duration
                    duration = duration > 99 ? 99 : duration;
                    yearDur += String.format("%02d", duration);
                } catch (Exception e) {
                    yearDur += "01";    // Default duration uses 01 (minimum value)
                }
            }
        }
        
        return yearDur;
    }

}
