package org.telcomp.sbb;

import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;
import javax.slee.nullactivity.NullActivity;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;

import net.java.slee.resource.sip.SleeSipProvider;

import org.telcomp.events.EndRegistrarTelcoServiceEvent;
import org.telcomp.events.EndSetDataTelcoServiceEvent;
import org.telcomp.events.StartSetDataTelcoServiceEvent;

public abstract class RegistrarSbb implements javax.slee.Sbb {
	
	private SleeSipProvider sipFactoryProvider;
	private MessageFactory messageFactory;
	private NullActivityFactory nullActivityFactory;
	private NullActivityContextInterfaceFactory nullACIFactory;

	public void onRegister(RequestEvent event, ActivityContextInterface aci) {
		System.out.println("*******************************************");
		System.out.println("RegistrarTelcoService Invoked");
		
		this.setUserActivity(aci);
		Request request = event.getRequest();
		ContactHeader contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
		FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
		String name = this.getName(fromHeader.getAddress().toString());
		this.setUserId(name);
		ExpiresHeader expires = (ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
		//Checking if user is registering or unregistering with or without Expires Header
		if(contactHeader.getExpires() != -1){
			//Registering or unregistering without Expires Header
			if(contactHeader.getExpires() == 0){
				System.out.println("Unregistering User "+name);
				this.unregisterUser(name, request, aci);
			} else{
				System.out.println("Registering User "+name);
				this.registerUser(contactHeader, name, request, aci);
			}
		} else{
			//registering or unregistering with Expires Header
			if(expires.getExpires() == 0){
				System.out.println("Unregistering User "+name);
				this.unregisterUser(name, request, aci);
			} else{
				System.out.println("Registering User "+name);
				this.registerUser(contactHeader, name, request, aci);
			}
		}
		aci.detach(this.sbbContext.getSbbLocalObject());
	}
	
	public void onEndSetDataTelcoServiceEvent(EndSetDataTelcoServiceEvent event, ActivityContextInterface aci) {
		if(this.getRegisterActivity() != null){
			if(aci.getActivity().equals(this.getRegisterActivity().getActivity())){
				if(event.isCommited()){
					this.respondUserAgent(Response.OK);
					ActivityContextInterface nullAci = this.createNullActivityACI();
					HashMap<String, Object> operationInputs = new HashMap<String, Object>();
					operationInputs.put("userID", (String) this.getUserId());
					operationInputs.put("operation", (String) "register");
					EndRegistrarTelcoServiceEvent endRegistrar = new EndRegistrarTelcoServiceEvent(operationInputs);
					this.fireEndRegistrarTelcoServiceEvent(endRegistrar, nullAci, null);
					System.out.println("Output UserID = "+this.getUserId());
					System.out.println("Output Operation = Register");
					System.out.println("*******************************************");
				} else {
					this.respondUserAgent(Response.NOT_FOUND);
				}
			}
		} else if(this.getUnregisterActivity() != null){
			if(aci.getActivity().equals(this.getUnregisterActivity().getActivity())){
				this.respondUserAgent(Response.OK);
				ActivityContextInterface nullAci = this.createNullActivityACI();
				HashMap<String, Object> operationInputs = new HashMap<String, Object>();
				operationInputs.put("userID", (String) this.getUserId());
				operationInputs.put("operation", (String) "unregister");
				EndRegistrarTelcoServiceEvent endRegistrar = new EndRegistrarTelcoServiceEvent(operationInputs);
				this.fireEndRegistrarTelcoServiceEvent(endRegistrar, nullAci, null);
				System.out.println("Output UserID = "+this.getUserId());
				System.out.println("Output Operation = Unregister");
				System.out.println("*******************************************");
			}
		}
		aci.detach(this.sbbContext.getSbbLocalObject());
	}
	
	private void unregisterUser(String name, Request request, ActivityContextInterface aci){
		try {
			HashMap<String, Object> operationInputs = new HashMap<String, Object>();
			operationInputs.put("parameter", (String) "state");
			operationInputs.put("value", (String) "offline");
			operationInputs.put("identification", (String) name);
			StartSetDataTelcoServiceEvent startDataAccess = new StartSetDataTelcoServiceEvent(operationInputs);
			ActivityContextInterface unregisterAci = this.createNullActivityACI();
			this.setUnregisterActivity(unregisterAci);
			unregisterAci.attach(this.sbbContext.getSbbLocalObject());
			this.fireStartSetDataTelcoServiceEvent(startDataAccess, unregisterAci, null);
		} catch (Exception e) {
			System.out.println("Exception while processing MESSAGE: "+ e.getMessage());
		}
	}
	
	private void registerUser(ContactHeader contactHeader, String name, Request request, ActivityContextInterface aci){
		String contacturi = contactHeader.getAddress().getURI().toString();
		try {
			HashMap<String, Object> operationInputs1 = new HashMap<String, Object>();
			operationInputs1.put("parameter", (String) "sipuri");
			operationInputs1.put("value", (String) contacturi);
			operationInputs1.put("identification", (String) name);
			StartSetDataTelcoServiceEvent startDataAccess = new StartSetDataTelcoServiceEvent(operationInputs1);
			
			HashMap<String, Object> operationInputs2 = new HashMap<String, Object>();
			operationInputs2.put("parameter", (String) "state");
			operationInputs2.put("value", (String) "online");
			operationInputs2.put("identification", (String) name);
			StartSetDataTelcoServiceEvent startDataAccess1 = new StartSetDataTelcoServiceEvent(operationInputs2);
			
			ActivityContextInterface registerAci = this.createNullActivityACI();
			this.setRegisterActivity(registerAci);
			ActivityContextInterface stateAci = this.createNullActivityACI();

			registerAci.attach(this.sbbContext.getSbbLocalObject());
			stateAci.attach(this.sbbContext.getSbbLocalObject());
			
			this.fireStartSetDataTelcoServiceEvent(startDataAccess, registerAci, null);
			this.fireStartSetDataTelcoServiceEvent(startDataAccess1, stateAci, null);
		} catch (Exception e) {
			System.out.println("Exception while processing MESSAGE: "+ e.getMessage());
		}
	}
	
	private void respondUserAgent(int responseName){
		try {
			ServerTransaction st = (ServerTransaction) this.getUserActivity().getActivity();
			Response response = messageFactory.createResponse(responseName, st.getRequest());
			st.sendResponse(response);
			st.terminate();
			this.getUserActivity().detach(this.sbbContext.getSbbLocalObject());
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private ActivityContextInterface createNullActivityACI() {
		NullActivity nullActivity = this.nullActivityFactory.createNullActivity();
		ActivityContextInterface nullActivityACI = null;
		nullActivityACI = this.nullACIFactory.getActivityContextInterface(nullActivity);
		return nullActivityACI;
	}
	
	private String getName(String prevName){
		return prevName.substring(prevName.indexOf(':')+1, prevName.indexOf('@'));
	}


	
	// TODO: Perform further operations if required in these methods.
	public void setSbbContext(SbbContext context) { 
		this.sbbContext = context;
		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			sipFactoryProvider = (SleeSipProvider) ctx.lookup("slee/resources/jainsip/1.2/provider");
			messageFactory = sipFactoryProvider.getMessageFactory();
			nullActivityFactory = (NullActivityFactory) ctx.lookup("slee/nullactivity/factory");
			nullACIFactory = (NullActivityContextInterfaceFactory) ctx.lookup("slee/nullactivity/activitycontextinterfacefactory");
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
    public void unsetSbbContext() { this.sbbContext = null; }
    
    // TODO: Implement the lifecycle methods if required
    public void sbbCreate() throws javax.slee.CreateException {}
    public void sbbPostCreate() throws javax.slee.CreateException {}
    public void sbbActivate() {}
    public void sbbPassivate() {}
    public void sbbRemove() {}
    public void sbbLoad() {}
    public void sbbStore() {}
    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface activity) {}
    public void sbbRolledBack(RolledBackContext context) {}
    
    public abstract void setUserActivity(ActivityContextInterface aci);
    public abstract ActivityContextInterface getUserActivity();
    public abstract void setRegisterActivity(ActivityContextInterface aci);
    public abstract ActivityContextInterface getRegisterActivity();
    public abstract void setUnregisterActivity(ActivityContextInterface aci);
    public abstract ActivityContextInterface getUnregisterActivity();
    public abstract void setUserId(String userId);
    public abstract String getUserId();
    
	public abstract void fireEndRegistrarTelcoServiceEvent (EndRegistrarTelcoServiceEvent event, ActivityContextInterface aci, Address address);
	public abstract void fireStartSetDataTelcoServiceEvent (StartSetDataTelcoServiceEvent event, ActivityContextInterface aci, Address address);

	
	/**
	 * Convenience method to retrieve the SbbContext object stored in setSbbContext.
	 * 
	 * TODO: If your SBB doesn't require the SbbContext object you may remove this 
	 * method, the sbbContext variable and the variable assignment in setSbbContext().
	 *
	 * @return this SBB's SbbContext object
	 */
	
	protected SbbContext getSbbContext() {
		return sbbContext;
	}

	private SbbContext sbbContext; // This SBB's SbbContext

}
