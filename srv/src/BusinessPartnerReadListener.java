package com.sap.cap.capbizservice.handlers;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.sdk.service.prov.api.operations.Query;
import com.sap.cloud.sdk.service.prov.api.operations.Read;
import com.sap.cloud.sdk.service.prov.api.request.OrderByExpression;
import com.sap.cloud.sdk.service.prov.api.request.QueryRequest;
import com.sap.cloud.sdk.service.prov.api.request.ReadRequest;
import com.sap.cloud.sdk.service.prov.api.response.ErrorResponse;
import com.sap.cloud.sdk.service.prov.api.response.QueryResponse;
import com.sap.cloud.sdk.service.prov.api.response.ReadResponse;

import com.sap.cloud.sdk.s4hana.*;
import com.sap.cloud.sdk.cloudplatform.connectivity.*;

import cds.gen.cloud.sdk.capng.CapBusinessPartner;
import cds.gen.cloud.sdk.capng.CapBusinessPartner_;
import cds.gen.cloud.sdk.capng.Capng_;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.*;

import com.sap.cloud.sdk.odatav2.connectivity.ODataException;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.DefaultBusinessPartnerService;

@Component
@ServiceName("cloud.sdk.capng")
public class BusinessPartnerReadListener implements EventHandler {

    private final HttpDestination httpDestination = DestinationAccessor.getDestination("S4H").asHttp();

    @On(event = CdsService.EVENT_READ, entity = "cloud.sdk.capng.CapBusinessPartner")
    public void onRead(CdsReadEventContext context) throws ODataException {

        final Map<Object, Map<String, Object>> result = new HashMap<>();
        final List<BusinessPartner> businessPartners =
                new DefaultBusinessPartnerService().getAllBusinessPartner().top(10).execute(httpDestination);

        final List<CapBusinessPartner> capBusinessPartners =
                convertS4BusinessPartnersToCapBusinessPartners(businessPartners, "S4H");
        capBusinessPartners.forEach(capBusinessPartner -> {
            result.put(capBusinessPartner.getId(), capBusinessPartner);
        });

        context.setResult(result.values());
    }

    @On(event = CdsService.EVENT_CREATE, entity = "cloud.sdk.capng.CapBusinessPartner")
    public void onCreate(CdsCreateEventContext context) throws ODataException {
        final BusinessPartnerService service = new DefaultBusinessPartnerService();

        Map<String, Object> m = context.getCqn().entries().get(0);
        BusinessPartner bp = BusinessPartner.builder().
            firstName(m.get("firstName").toString()).
            lastName(m.get("surname").toString()).
            businessPartner(m.get("ID").toString()).
            build();

        service.createBusinessPartner(bp).execute(httpDestination);
    }

    private List<CapBusinessPartner> convertS4BusinessPartnersToCapBusinessPartners(
            final List<BusinessPartner> s4BusinessPartners,
            final String destinationName) {
        final List<CapBusinessPartner> capBusinessPartners = new ArrayList<>();

        for (final BusinessPartner s4BusinessPartner : s4BusinessPartners) {
            final CapBusinessPartner capBusinessPartner = com.sap.cds.Struct.create(CapBusinessPartner.class);

            capBusinessPartner.setFirstName(s4BusinessPartner.getFirstName());
            capBusinessPartner.setSurname(s4BusinessPartner.getLastName());
            capBusinessPartner.setId(s4BusinessPartner.getBusinessPartner());
            capBusinessPartner.setSourceDestination(destinationName);

            capBusinessPartners.add(capBusinessPartner);
        }

        return capBusinessPartners;
    }
}