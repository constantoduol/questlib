package com.quest.access.useraccess.services;

import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import com.quest.access.crypto.Security;
import com.quest.servlets.ClientWorker;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by conny on 6/23/15.
 */
public class Activation {
    
    
    
    private static char [] secret = new char[]{'K','Z','M','1','3','A','J','H','N','O','C','Q','R','K','Z','N','U'};
    
    
    

    private static Integer[] prodSum(String expiry){
        Integer sum=0;
        Integer prod=1;
        for(int x=0; x<expiry.length(); x++){
            Character ch=expiry.charAt(x);
            Integer num=Integer.parseInt(ch.toString());
            sum=sum+num;
            if(num!=0){
                prod=prod*num;
            }
        }
        return new Integer[]{sum,prod};
    }

    private static Object[] decodeKey(String key,String name){
        try{
            String startExpiry=key.substring(14,16);
            String secondExpiry=key.substring(7,9);
            String thirdExpiry=key.substring(0,2);
            String fourthExpiry=key.substring(19,21);
            StringBuilder builder1=new StringBuilder();
            builder1.append(startExpiry).append(secondExpiry).append(thirdExpiry).append(fourthExpiry);
            String expiry=builder1.toString();
            Integer [] vals=prodSum(expiry);
            String prod=vals[1].toString();
            String sum=vals[0].toString();
            if(prod.length()>2){
                prod=prod.substring(0, 2);
            } else if (prod.length()<2){
                prod=prod+"0";
            }

            if (sum.length()>2){
                sum=sum.substring(0,2);
            }
            else if(sum.length()<2){
                sum=sum+"0";
            }

            String prodFromKey = key.substring(21,23);
            String sumFromKey = key.substring(23);
            String accountHash = Security.toBase64(Security.makePasswordDigest(name, secret)).substring(0,6);
            accountHash=accountHash.replace("/", "5");
            accountHash=accountHash.replace("+", "3").toUpperCase();
            String accountHashFromKey=key.substring(11,14)+key.substring(16,19);
            String servicesHash = key.substring(9,11)+key.substring(2,7);

            if(prodFromKey.equals(prod) && sumFromKey.equals(sum) && accountHash.equals(accountHashFromKey)){
                return new Object[]{expiry};
            }
            return null;
        }
        catch(Exception e){

        }
        return null;
    }


    public ClientWorker validateKey(Server serv, ClientWorker worker){
        try {
            Database db = new Database("user_server", worker.getSession());
            JSONObject requestData = worker.getRequestData();
            String name = requestData.optString("business_name");
            String key = requestData.optString("activation_key");
            Object[] activeData = decodeKey(key, name);
            JSONObject data = new JSONObject();
            if(activeData == null){
                worker.setResponseData(Message.FAIL);
                worker.setReason("Invalid activation key or business name ");
                serv.messageToClient(worker);
            }
            else{
                try {
                    db.execute("DELETE FROM ACTIVATION_DATA");
                    db.doInsert("ACTIVATION_DATA", new String[]{key,name,"!NOW()"});
                    data.put("expiry", activeData[0]+"00000");
                    String vNo = serv.getConfig().getInitParameter("version-no");
                    String vName = serv.getConfig().getInitParameter("version-name");
                    data.put("version_no", vNo);
                    data.put("version_name", vName);
                    worker.setResponseData(data);
                    serv.messageToClient(worker);
                } catch (Exception ex) {
                    Logger.getLogger(Activation.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } catch (Exception ex) {
            Logger.getLogger(Activation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return worker;
    }
}
