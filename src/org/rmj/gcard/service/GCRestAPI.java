package org.rmj.gcard.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.integsys.pojo.UnitGCApplication;
import org.rmj.replication.utility.WebClient;
/**
 *
 * @author sayso
 */
public class GCRestAPI {
    private static String GCARD_URL_REQUEST_NEW = "gcard/ms/request_new_gcard.php";
    private static String GCARD_URL_UPDATE_POINT = "gcard/ms/update_gcard_point.php";
    private static String GCARD_URL_REQUEST_OFFLINE = "gcard/ms/request_offline_trans.php";
    private static String GCARD_URL_REQUEST_OFFLINE_HISTORY = "/gcard/ms/request_offline_history.php";
    private static String GCARD_URL_POST_OFFLINE = "gcard/ms/post_offline_trans.php";
    private static String GCARD_URL_REQUEST_ORDER_INFO = "gcard/ms/request_order_info.php";
    private static String GCARD_URL_REQUEST_CLUB_MEMBERS = "gcard/ms/request_club_member.php";
    private static String GCARD_URL_POINT_TRANSFER = "gcard/ms/point_transfer.php";
    private static String GCARD_URL_VERIFY_OFFLINE_ENTRY = "gcard/ms/verify_offline_points.php";
    private static String GCARD_URL_UPLOAD_TDS = "gcard/ms/dgcard_upload_transaction.php";
    private static String GCARD_URL_POINTS_UPDATE = "gcard/ms/dgcard_upload_transaction.php";
    
    private static Map getHeaders(GRider foGRider){
        Calendar calendar = Calendar.getInstance();
        //Create the header section needed by the API
        
        Map<String, String> headers = new HashMap<String, String>();
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
        
        return headers;
    }
    
    private static Map getHeaders(GRider foGRider, String fsMobileNo){
        Map<String, String> headers = getHeaders(foGRider);
        
        if (!fsMobileNo.equals(""))
            if (fsMobileNo.contains("+63"))
                fsMobileNo = fsMobileNo.replace("+63", "0");
        
        headers.put("g-api-mobile", fsMobileNo);
        
        return headers;
    }
    
    public static JSONObject ApproveApplication(GRider foGRider, UnitGCApplication foGCApp, String fsMobileNo){
        try {
            //Create the parameters needed by the API
            JSONObject param = new JSONObject();
            param.put("transnox", foGCApp.getTransNo());
            param.put("transact", SQLUtil.dateFormat(foGCApp.getTransactDate(), SQLUtil.FORMAT_SHORT_DATE));
            param.put("sourcecd", foGCApp.getSource());
            param.put("purcmode", foGCApp.getPurchaseMode());
            param.put("clientid", foGCApp.getClientID());
            param.put("cardtype", foGCApp.getCardType());
            param.put("appltype", foGCApp.getApplicationType());
            param.put("prevgcrd", foGCApp.getPreviousGCard());
            param.put("sourceno", foGCApp.getSourceNo());
            param.put("amtpaidx", foGCApp.getAmountPaid());
            param.put("compnyid", foGCApp.getCompanyID());
            param.put("serialid", foGCApp.getSerialID());
            param.put("yellowxx", foGCApp.getYellow());
            param.put("whitexxx", foGCApp.getWhite());
            param.put("pointsxx", foGCApp.getPoints());
            param.put("digitalx", foGCApp.getDigital());
            
            System.out.println(param.toJSONString());
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_NEW;
            String response = WebClient.httpPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) getHeaders(foGRider, fsMobileNo));
            
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;
            }
            json_obj = (JSONObject) oParser.parse(response);
            return json_obj;
        } catch (IOException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", "IO Exception: " + ex.getMessage());
            err_detl.put("code", "250");
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        } catch (ParseException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", "Parse Exception: " + ex.getMessage());
            err_detl.put("code", ex.getErrorType());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        }    
    }
    
    public static JSONObject UploadTDS(GRider foGRider, String fsTransNox){
        try {
            String lsSQL = "SELECT" +
                                "  sTransNox" +
                                ", sGCardNox" +
                                ", dTransact" +
                                ", sBranchCd" +
                                ", sSourceNo" +
                                ", sSourceCd" +
                                ", sOTPasswd" +
                                ", nTranAmtx" +
                                ", nPointsxx" +
                                ", cSendStat" +
                                ", cTranStat" +
                                ", sEntryByx" +
                                ", dEntryDte" +
                            " FROM G_Card_Digital_Transaction" +
                            " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
            
            ResultSet loRS = foGRider.executeQuery(lsSQL);
            
            if (!loRS.next()){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", "No record found. - " + fsTransNox);
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;
            }
            
            //Create the parameters needed by the API
            JSONObject param = new JSONObject();
            param.put("sTransNox", loRS.getString("sTransNox"));
            param.put("sGCardNox", loRS.getString("sGCardNox"));
            param.put("dTransact", loRS.getString("dTransact"));
            param.put("sBranchCd", loRS.getString("sBranchCd"));
            param.put("sSourceNo", loRS.getString("sSourceNo"));
            param.put("sSourceCd", loRS.getString("sSourceCd"));
            param.put("sOTPasswd", loRS.getString("sOTPasswd"));
            param.put("nTranAmtx", loRS.getDouble("nTranAmtx"));
            param.put("nPointsxx", loRS.getDouble("nPointsxx"));
            param.put("cSendStat", loRS.getString("cSendStat"));
            param.put("cTranStat", loRS.getString("cTranStat"));
            param.put("sEntryByx", loRS.getString("sEntryByx"));
            param.put("dEntryDte", loRS.getString("dEntryDte"));           
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_UPLOAD_TDS;
            String response = WebClient.httpPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) getHeaders(foGRider));
            
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;
            }
            json_obj = (JSONObject) oParser.parse(response);
            return json_obj;
        } catch (IOException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", "IO Exception: " + ex.getMessage());
            err_detl.put("code", "250");
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        } catch (SQLException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", "SQL Exception: " + ex.getMessage());
            err_detl.put("code", "250");
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        } catch (ParseException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", "Parse Exception: " + ex.getMessage());
            err_detl.put("code", ex.getErrorType());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
            return err_mstr;
        }    
    }
    
    //Update points of a particular GCard... 
    public static JSONObject UpdatePoint(GRider foGRider, String fsCardNmbr, String fsDescript, String fsReferNox, double fnPointsxx){
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
            param.put("secureno", MySQLAESCrypt.Encrypt(fsCardNmbr, MiscUtil.getPCName()));
            param.put("refernox", fsReferNox);
            param.put("descript", fsDescript);
            param.put("pointsxx", fnPointsxx);
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_UPDATE_POINT;
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
    
    public static JSONObject RequestOffline(GRider foGRider, String fsTransNox){
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
            param.put("transnox", MySQLAESCrypt.Encrypt(fsTransNox, MiscUtil.getPCName()));
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_OFFLINE;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;

            }
            json_obj = (JSONObject) oParser.parse(response);
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

    public static JSONObject RequestOfflineHistory(GRider foGRider, String fsCardNmbr, String fcTranStat){
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
            param.put("secureno", MySQLAESCrypt.Encrypt(fsCardNmbr, MiscUtil.getPCName()));
            param.put("transtat", fcTranStat);
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_OFFLINE_HISTORY;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;

            }
            json_obj = (JSONObject) oParser.parse(response);
            
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

    public static JSONObject RequestOrderInfo(GRider foGRider, String fsTransNox){
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
            param.put("uuid", MySQLAESCrypt.Encrypt(fsTransNox, MiscUtil.getPCName()));
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
            
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_ORDER_INFO;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;

            }
            json_obj = (JSONObject) oParser.parse(response);

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
    
    public static JSONObject PostOffline(GRider foGRider, String fsCardNmbr, String fsOTPasswd, String fsIMEINoxx, String fsMobileNo, String fsQRDateTm, ArrayList<String> fasTransNox){
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
            param.put("secureno", MySQLAESCrypt.Encrypt(fsCardNmbr, MiscUtil.getPCName()));
            param.put("transnox", fasTransNox);
            param.put("otpasswd", fsOTPasswd);
            param.put("imeinoxx", fsIMEINoxx);
            param.put("mobileno", fsMobileNo);
            param.put("qrdatetm", fsQRDateTm);
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
            
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_POST_OFFLINE;
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
    
    public static JSONObject RequestClubMembers(GRider foGRider, String fsCardNmbr){
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

            param.put("secureno", MySQLAESCrypt.Encrypt(fsCardNmbr, MiscUtil.getPCName()));           

            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
           
            
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_CLUB_MEMBERS;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);

            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));

                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);

                return err_mstr;
            }

            json_obj = (JSONObject) oParser.parse(response);

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
    
    public static JSONObject PointTransfer(GRider foGRider, String fsGCardNox, String fsTransNox, String fdTransact, long fnPointsxx, JSONArray detail){
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

            param.put("sGCardNox", fsGCardNox);
            param.put("sTransNox", fsTransNox);
            param.put("dTransact", fdTransact);
            param.put("nPointsxx", fnPointsxx);
            param.put("detail", detail);
            
            JSONParser oParser = new JSONParser();

            JSONObject json_obj = null;
           
            
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_POINT_TRANSFER;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);

            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));

                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);

                return err_mstr;
            }
            
            json_obj = (JSONObject) oParser.parse(response);

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
    
    public static JSONObject VerifyOfflineEntry(GRider foGRider, String sTransNox){        
        try {
            Calendar calendar = Calendar.getInstance();
            Map<String, String> headers = new HashMap<String, String>();
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
            headers.put("g-api-mobile", "");
            
            JSONObject param = new JSONObject();
            param.put("transno", sTransNox);
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_VERIFY_OFFLINE_ENTRY;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));

                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);

                return err_mstr;
            }
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = (JSONObject) oParser.parse(response);
            return json_obj;
        } catch (IOException | ParseException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("message", ex.getMessage());
            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "ERROR");
            err_mstr.put("error", err_detl);
 
            return err_mstr;
        }
    }
    
    /*
    public static JSONObject RequestOffline(GRider foGRider, String fsCardNmbr, String fcTranStat){
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
            param.put("secureno", MySQLAESCrypt.Encrypt(fsCardNmbr, MiscUtil.getPCName()));
            param.put("transtat", fcTranStat);
            
            JSONParser oParser = new JSONParser();
            JSONObject json_obj = null;
                        
            String lsAPI = CommonUtils.getConfiguration(foGRider, "WebSvr") + GCARD_URL_REQUEST_OFFLINE;
            String response = WebClient.httpsPostJSon(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);
            
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("message", System.getProperty("store.error.info"));
                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "ERROR");
                err_mstr.put("error", err_detl);
                return err_mstr;

            }
            json_obj = (JSONObject) oParser.parse(response);
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
    */
}
