package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import edu.yale.its.tp.cas.client.ProxyTicketValidator;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

/**
 * Callback implemented for an unit test or integration test called during
 * 
 * @author bjalon
 *
 */
public interface CallBackCheckTicketValidatorState {
    
    public boolean checkTicketValidatorState(ServiceTicketValidator stv);

    public boolean checkProxyTicketValidatorState(ProxyTicketValidator stv);

}
