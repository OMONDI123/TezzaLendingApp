package co.ke.tezza.loanapp.util;

import co.ke.tezza.loanapp.model.AuthResponse;

public class UserContext {
	
	private static final ThreadLocal<Long> AD_ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> AD_CLIENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> C_BPARTNER_ID = new ThreadLocal<>();
    
    private static final ThreadLocal<AuthResponse> AUTH_RESPONSE = new ThreadLocal<>();

    public static void setAD_Org_ID(long id) {
        AD_ORG_ID.set(id);
    }

    public static long getAD_Org_ID() {
        return AD_ORG_ID.get() != null ? AD_ORG_ID.get() : 0;
    }

    public static void setAD_Client_ID(long id) {
        AD_CLIENT_ID.set(id);
    }

    public static long getAD_Client_ID() {
        return AD_CLIENT_ID.get() != null ? AD_CLIENT_ID.get() : 0;
    }

    public static void setAUTH_RESPONSE(AuthResponse response) {
    	AUTH_RESPONSE.set(response);
    }

    public static AuthResponse getAUTH_RESPONSE() {
        return AUTH_RESPONSE.get() != null ? AUTH_RESPONSE.get() : null;
    }
    
    
    public static void setC_BPartner_ID(long id) {
        C_BPARTNER_ID.set(id);
    }

    public static long getC_BPartner_ID() {
        return C_BPARTNER_ID.get() != null ? C_BPARTNER_ID.get() : 0;
    }

    public static void clear() {
        AD_ORG_ID.remove();
        AD_CLIENT_ID.remove();
        C_BPARTNER_ID.remove();
        AUTH_RESPONSE.remove();
    }

}
