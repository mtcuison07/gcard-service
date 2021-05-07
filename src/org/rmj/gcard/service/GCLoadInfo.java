/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.replication.utility.WebClient;

/**
 *
 * @author sayso
 */
public class GCLoadInfo {    
    private static String GCARD_URL_GET_DEVINFO = "gcard/ms/request_gcard_info.php";
    
    public static JSONObject GetCardInfoOnline(GRider foGRider, String sCardNmbr){
        try {
            Calendar calendar = Calendar.getInstance();
            //Create the header section needed by the API
            Map<String, String> headers =
                    new HashMap<String, String>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/json");
            headers.put("g-api-id", foGRider.getProductID());
            headers.put("g-api-imei", MiscUtil.getPCName());
            headers.put("g-api-key", SQLUtil.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));
            headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
            headers.put("g-api-client", foGRider.getClientID());
            headers.put("g-api-user", foGRider.getUserID());
            headers.put("g-api-log", "");
            headers.put("g-api-token", "");

            //Create the parameters needed by the API
            JSONObject param = new JSONObject();
            param.put("secureno", MySQLAESCrypt.Encrypt(sCardNmbr, MiscUtil.getPCName()));

            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
            
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_GET_DEVINFO;
            String response = WebClient.httpPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;

            }
            json_obj = (JSONObject) oParser.parse(response);
            //System.out.println(json_obj.toJSONString());
            //System.out.println(response);
            return json_obj;
        } catch (IOException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", ex.getMessage());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        } catch (ParseException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", ex.getMessage());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        }
    }
    
    public static JSONObject GetCardInfoOffline(GRider foGRider, String fsCardNmbr){
        JSONObject poGCInfo = new JSONObject();
        String lsSQL = "SELECT" +
                            "  a.sGCardNox" +
                            ", a.sCardNmbr" +
                            ", a.cCardType" +
                            ", b.sNmOnCard" +
                            ", a.sClientID" +
                            ", c.sCompnyNm" +
                            ", CONCAT(c.sAddressx, ', ', d.sTownName, ', ', e.sProvName) sAddressx" +
                            ", CONCAT(f.sTownName, ', ', g.sProvName) sBirthPlc" +
                            ", c.dBirthDte" +
                            ", a.dMemberxx" +
                            ", a.dActivate" +
                            ", a.nTotPoint" +
                            ", a.nAvlPoint" +
                            ", a.sGroupIDx" +
                            ", a.cIndvlPts" +
                            ", a.cMainGrpx" +
                            ", a.cCardStat" +
                            ", IF(ISNULL(h.sEmployID), 0, 1) cEmployee" +
                            ", a.cDigitalx" +
                            ", c.sMobileNo" +
                        " FROM G_Card_Master a" +
                            "   LEFT JOIN G_Card_Application b ON a.sApplicNo = b.sTransNox" +
                            "   LEFT JOIN Client_Master c ON a.sClientID = c.sClientID" +
                            "   LEFT JOIN TownCity d ON c.sTownIDxx = d.sTownIDxx" +
                            "   LEFT JOIN Province e ON d.sProvIDxx = e.sProvIDxx" +
                            "   LEFT JOIN TownCity f ON c.sBirthPlc = f.sTownIDxx" +
                            "   LEFT JOIN Province g ON f.sProvIDxx = g.sProvIDxx" +
                            "   LEFT JOIN Employee_Master001 h ON a.sClientID = h.sEmployID AND h.cRecdStat = '1'" +
                        " WHERE a.sCardNmbr = " + SQLUtil.toSQL(fsCardNmbr);
                            
        ResultSet loRS = foGRider.executeQuery(lsSQL);
        
        try {
            if(!loRS.next()){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", "GCard Number is not found in the database...");
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;
            }
           
            //System.out.println(lsSQL);
            poGCInfo = new JSONObject();
            poGCInfo.put("result", "SUCCESS");
            poGCInfo.put("sGCardNox", loRS.getString("sGCardNox"));
            poGCInfo.put("sCardNmbr", loRS.getString("sCardNmbr"));
            poGCInfo.put("cCardType", loRS.getString("cCardType"));
            poGCInfo.put("sNmOnCard", loRS.getString("sNmOnCard"));
            poGCInfo.put("sClientID", loRS.getString("sClientID"));
            poGCInfo.put("sCompnyNm", loRS.getString("sCompnyNm"));
            poGCInfo.put("sAddressx", loRS.getString("sAddressx"));
            poGCInfo.put("sBirthPlc", loRS.getString("sBirthPlc"));
            poGCInfo.put("dBirthDte", loRS.getString("dBirthDte"));
            poGCInfo.put("dMemberxx", loRS.getString("dMemberxx"));
            poGCInfo.put("dActivate", loRS.getString("dActivate"));
            poGCInfo.put("nTotPoint", loRS.getDouble("nTotPoint"));
            poGCInfo.put("nAvlPoint", loRS.getDouble("nAvlPoint"));
            poGCInfo.put("sGroupIDx", loRS.getString("sGroupIDx"));
            poGCInfo.put("cIndvlPts", loRS.getString("cIndvlPts"));
            poGCInfo.put("cMainGrpx", loRS.getString("cMainGrpx"));
            poGCInfo.put("cCardStat", loRS.getString("cCardStat"));
            poGCInfo.put("cEmployee", loRS.getString("cEmployee"));
            poGCInfo.put("cDigitalx", loRS.getString("cDigitalx"));
            poGCInfo.put("sMobileNo", loRS.getString("sMobileNo")); //mac 2020.03.31
            //poGCInfo.put("dCardExpr", loRS.getDate("dCardExpr"));
        } catch (SQLException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", ex.getMessage());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        }

        return poGCInfo;
    }
    
}
