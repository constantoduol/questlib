package com.quest.access.useraccess;

import com.quest.access.common.UniqueRandom;
import com.quest.access.control.Server;
import com.quest.servlets.ClientWorker;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author constant oduol
 * @version 1.0(10/5/12)
 */
/**
 * <p>
 * This file defines a framework for providing services through a server
 * dividing the activities of a server into services splits up the servers work
 * and ensures that the logic of the different services is easily maintained in
 * different class files. When a programmer writes a service class he
 * initializes the service by creating an instance of the service class with the
 * specified details. When the server starts it reads all the services
 * registered for this server into memory and therefore whenever a client
 * requests for any service, the server checks whether the service exists, if it
 * exists the server gets the location of the service class and then invokes the
 * provideService method in the specific service class.
 * </p>
 * <p>
 * Service providing classes must implement the Serviceable interface and
 * provide an implementation for the provideService() method. Services normally
 * have a permanent privilege associated with them and therefore if a user has
 * access to a service it means he has the corresponding permanent privilege,
 * revoking this privilege from the user renders him unable to access the
 * corresponding service. Every client's request to a service runs in a
 * different thread which invokes the specified service and returns the result
 * to the client, this means that service classes should be designed with multi
 * threading in mind since different threads may call a service class at the
 * same time
 * </p>
 *
 */
public class Service {

    /**
     * this is the unique id of a service
     */
    private String serviceId;
    /**
     * this is the name of the service
     */
    private String name;
    /**
     * the permanent privilege associated with this service
     */
    private String priv;

    /**
     * this is the class that has the implementation of this service
     */
    private Class serviceClass;

    /**
     * this constructs a service object and stores the location and name of the
     * service in the SERVICES table in the specific server's database
     *
     * @param name the name of the service, this is the name the clients use
     * when requesting for a service
     * @param serviceClass this is the class with the logic for the service so
     * if a client makes a request for this service the code in this class is
     * called
     * @param serv this is the server in whose scope this service is defined,
     * therefore a different server cannot recognize this service because it
     * does not belong to it
     */
    public Service(String name, Class serviceClass, Server serv) {
        UniqueRandom ur = new UniqueRandom(10);
        String serviceID = ur.nextRandom();
        this.serviceId = serviceID;
        this.serviceClass = serviceClass;
        try {
            this.priv = name;
            serv.startService(serviceClass.getName());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void runOnCreate() {
        try {
            Method method = serviceClass.getMethod("onCreate", (Class[]) null);
            Object newInstance = serviceClass.newInstance();
            method.invoke(newInstance, (Object[]) null);
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void runOnStart(Server serv) {
        try {
            Method method = serviceClass.getMethod("onStart", new Class[]{Server.class});
            Object newInstance = serviceClass.newInstance();
            method.invoke(newInstance, new Object[]{serv});
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     public void runOnPreExecute(Server serv,ClientWorker worker) {
        try {
            Method method = serviceClass.getMethod("onPreExecute", new Class[]{Server.class,ClientWorker.class});
            Object newInstance = serviceClass.newInstance();
            method.invoke(newInstance, new Object[]{serv,worker});
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Service(String name, String priv, Class clazz, String servId) {
        this.name = name;
        this.priv = priv;
        this.serviceClass = clazz;
        this.serviceId = servId;
    }

    /**
     * this method returns the permanent privilege associated with a service
     */
    public String getServicePrivilege() {
        return this.priv;
    }

    /**
     * this method returns the unique system generated id for this service
     */
    public String getServiceId() {
        return this.serviceId;
    }

    /**
     * this method returns the class that has an implementation for this service
     */
    public Class getServiceClass() {
        return this.serviceClass;
    }

    /**
     * this method returns the name of this service
     */

    public String getServiceName() {
        return this.name;
    } 

}
