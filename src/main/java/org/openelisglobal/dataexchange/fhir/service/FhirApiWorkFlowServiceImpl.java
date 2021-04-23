package org.openelisglobal.dataexchange.fhir.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.fhir.service.TaskWorker.TaskResult;
import org.openelisglobal.dataexchange.order.action.DBOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;

@Service
public class FhirApiWorkFlowServiceImpl implements FhirApiWorkflowService {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private FhirReferralService fhirReferralService;

    @Value("${org.openelisglobal.fhirstore.uri}")
    private String localFhirStorePath;
    @Value("${org.openelisglobal.remote.source.uri}")
    private String[] remoteStorePaths;
    @Value("${org.openelisglobal.remote.source.updateStatus}")
    private Optional<Boolean> remoteStoreUpdateStatus;
    @Value("${org.openelisglobal.remote.source.identifier:}#{T(java.util.Collections).emptyList()}")
    private List<String> remoteStoreIdentifier;

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 2 * 60 * 1000)
    @Override
    public void pollForRemoteTasks() {
        processWorkflow(ResourceType.Task);
    }

    @Override
    @Async
    public void processWorkflow(ResourceType resourceType) {
        for (String remoteStorePath : remoteStorePaths) {
            switch (resourceType) {
            case Task:
                try {
                    beginTaskImportOrderPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getName(), "processWorkflow", e.getMessage());
                }
                try {
                    beginTaskCheckIfAcceptedPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getName(), "processWorkflow", e.getMessage());
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(this.getClass().getName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getName(), "processWorkflow", e.getMessage());
                }
                try {
                    beginTaskImportResultsPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getName(), "processWorkflow", e.getMessage());
                }
            default:
            }
        }
    }

    private void beginTaskCheckIfAcceptedPath(String remoteStorePath) throws FhirLocalPersistingException {
        if (remoteStoreIdentifier.isEmpty()) {
            return;
        }

        Map<String, Resource> updateResources = new HashMap<>();

        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        Bundle originalTasksBundle = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
//                .revInclude(Task.INCLUDE_BASED_ON)//
                .where(Task.STATUS.exactly().code(TaskStatus.REQUESTED.toCode()))//
                .where(Task.REQUESTER.hasAnyOfIds(remoteStoreIdentifier))//
                .execute();
        Map<String, Task> originalTasksById = new HashMap<>();
        for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
            if (bundleEntry.hasResource() && bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
                Task originalTask = (Task) bundleEntry.getResource();
                originalTasksById.put(originalTask.getIdElement().getIdPart(), originalTask);
            }
        }
        if (originalTasksById.size() <= 0) {
            return;
        }

        Bundle tasksBasedOnOriginalTasksBundle = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
//                .revInclude(Task.INCLUDE_BASED_ON)//
                .where(Task.BASED_ON.hasAnyOfIds(originalTasksById.keySet()))
                .where(Task.STATUS.exactly().code(TaskStatus.ACCEPTED.toCode()))//
                .execute();
        List<Task> acceptedTasks = new ArrayList<>();
        for (BundleEntryComponent bundleEntry : tasksBasedOnOriginalTasksBundle.getEntry()) {
            if (bundleEntry.hasResource() && bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
                Task taskBasedOnOrginalTask = (Task) bundleEntry.getResource();
                Task originalTask = originalTasksById
                        .get(taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart());
                originalTask.setStatus(TaskStatus.ACCEPTED);
                updateResources.put(originalTask.getIdElement().getIdPart(), originalTask);
                acceptedTasks.add(originalTasksById
                        .get(taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart()));
            }
        }

        tasksBasedOnOriginalTasksBundle = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
//                .revInclude(Task.INCLUDE_BASED_ON)//
                .where(Task.BASED_ON.hasAnyOfIds(originalTasksById.keySet()))
                .where(Task.STATUS.exactly().code(TaskStatus.REJECTED.toCode()))//
                .execute();
        List<Task> rejectedTasks = new ArrayList<>();
        for (BundleEntryComponent bundleEntry : tasksBasedOnOriginalTasksBundle.getEntry()) {
            if (bundleEntry.hasResource() && bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
                Task taskBasedOnOrginalTask = (Task) bundleEntry.getResource();
                Task originalTask = originalTasksById
                        .get(taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart());
                originalTask.setStatus(TaskStatus.REJECTED);
                updateResources.put(originalTask.getIdElement().getIdPart(), originalTask);
                rejectedTasks.add(originalTask);
            }
        }

        fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    private void beginTaskImportResultsPath(String remoteStorePath) {
        if (remoteStoreIdentifier.isEmpty()) {
            return;
        }

        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        IQuery<Bundle> searchQuery = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
//                .revInclude(Task.INCLUDE_BASED_ON)//
                .include(Task.INCLUDE_BASED_ON) // serviceRequest
                .where(Task.STATUS.exactly().code(TaskStatus.ACCEPTED.toCode()))//
                .where(Task.REQUESTER.hasAnyOfIds(remoteStoreIdentifier));
        Bundle originalTasksBundle = searchQuery.execute();
        Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest = new HashMap<>();
        for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
            if (bundleEntry.hasResource()) {
                try {
                    addOriginalReferralObject(bundleEntry, originalReferralObjectsByServiceRequest);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                    LogEvent.logError("could not import result for: " + bundleEntry.getResource().getId(), e);
                }
            }
        }
        if (originalReferralObjectsByServiceRequest.size() <= 0) {
            return;
        }

        searchQuery = sourceFhirClient.search()//
                .forResource(ServiceRequest.class)//
                .returnBundle(Bundle.class)//
//                .revInclude(Task.INCLUDE_BASED_ON)//
//                .include(ServiceRequest.INCLUDE_SPECIMEN) // specimen
                .revInclude(Observation.INCLUDE_BASED_ON.asRecursive())//
                .revInclude(DiagnosticReport.INCLUDE_BASED_ON.asRecursive())//
                .where(ServiceRequest.STATUS.exactly().code(ServiceRequestStatus.COMPLETED.toCode()))
                .where(ServiceRequest.BASED_ON.hasAnyOfIds(originalReferralObjectsByServiceRequest.keySet()));
        originalTasksBundle = searchQuery.execute();
        Map<String, ReferralResultsImportObjects> resultImportByServiceRequest = new HashMap<>();
        for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
            if (bundleEntry.hasResource()) {
                try {
                    addResultImportObject(bundleEntry, resultImportByServiceRequest,
                            originalReferralObjectsByServiceRequest);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                    LogEvent.logError("could not import result for: " + bundleEntry.getResource().getId(), e);
                }
            }
        }

        for (Entry<String, ReferralResultsImportObjects> resultsImportEntry : resultImportByServiceRequest.entrySet()) {
            try {
                fhirReferralService.setReferralResult(resultsImportEntry.getValue());
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                LogEvent.logError("could not import result for ServiceRequest: " + resultsImportEntry.getKey(), e);
            }
        }

    }

    private void addOriginalReferralObject(BundleEntryComponent bundleEntry,
            Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest) {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.ServiceRequest)) {
            ServiceRequest originalServiceRequest = (ServiceRequest) bundleEntry.getResource();
            String serviceRequestId = originalServiceRequest.getIdElement().getIdPart();
            OriginalReferralObjects referralObjects = originalReferralObjectsByServiceRequest
                    .getOrDefault(serviceRequestId, new OriginalReferralObjects());
            referralObjects.serviceRequest = originalServiceRequest;
            originalReferralObjectsByServiceRequest.put(serviceRequestId, referralObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
            Task referralTask = (Task) bundleEntry.getResource();
            String serviceRequestId = referralTask.getBasedOnFirstRep().getReferenceElement().getIdPart();
            OriginalReferralObjects referralObjects = originalReferralObjectsByServiceRequest
                    .getOrDefault(serviceRequestId, new OriginalReferralObjects());
            referralObjects.task = referralTask;
            originalReferralObjectsByServiceRequest.put(serviceRequestId, referralObjects);
        }

    }

    private void addResultImportObject(BundleEntryComponent bundleEntry,
            Map<String, ReferralResultsImportObjects> resultImportByServiceRequest,
            Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest) {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.ServiceRequest)) {
            ServiceRequest srBasedOnOriginalSR = (ServiceRequest) bundleEntry.getResource();
            String srId = srBasedOnOriginalSR.getIdElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.serviceRequest = srBasedOnOriginalSR;
            importObjects.originalReferralObjects = originalReferralObjectsByServiceRequest
                    .get(srBasedOnOriginalSR.getBasedOnFirstRep().getReferenceElement().getIdPart());

            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.Observation)) {
            Observation observation = (Observation) bundleEntry.getResource();

            String srId = observation.getBasedOnFirstRep().getReferenceElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.observations.add(observation);
            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.DiagnosticReport)) {
            DiagnosticReport diagnosticReport = (DiagnosticReport) bundleEntry.getResource();

            String srId = diagnosticReport.getBasedOnFirstRep().getReferenceElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.diagnosticReport = diagnosticReport;
            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
    }

    private void beginTaskImportOrderPath(String remoteStorePath) {
        if (remoteStoreIdentifier.isEmpty()) {
            return;
        }
        LogEvent.logDebug(this.getClass().getName(), "beginTaskPath", "searching for Tasks");
        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        IQuery<Bundle> searchQuery = sourceFhirClient.search()//
                .forResource(Task.class)//
                .returnBundle(Bundle.class)//
                // TODO use include
//                .include(Task.INCLUDE_PATIENT)//
//                .include(Task.INCLUDE_BASED_ON)//
                .where(Task.STATUS.exactly().code(TaskStatus.REQUESTED.toCode()))//
                .where(Task.OWNER.hasAnyOfIds(remoteStoreIdentifier));
        Bundle bundle = searchQuery.execute();

        if (bundle.hasEntry()) {
            LogEvent.logDebug(this.getClass().getName(), "beginTaskPath",
                    "received bundle with " + bundle.getEntry().size() + " entries");
        } else {
            LogEvent.logDebug(this.getClass().getName(), "beginTaskPath", "received bundle with 0 entries");
        }
        for (BundleEntryComponent bundleComponent : bundle.getEntry()) {

            if (bundleComponent.hasResource()
                    && ResourceType.Task.equals(bundleComponent.getResource().getResourceType())) {

                Task remoteTask = (Task) bundleComponent.getResource();
                try {
                    processTaskImportOrder(remoteTask, remoteStorePath, sourceFhirClient, bundle);
                } catch (RuntimeException | FhirLocalPersistingException e) {
                    LogEvent.logError(e);
                    LogEvent.logError(this.getClass().getName(), "beginTaskPath",
                            "could not process Task with identifier : " + remoteTask.getId());
                }

            }
        }
    }

    private void processTaskImportOrder(Task remoteTask, String remoteStorePath, IGenericClient sourceFhirClient,
            Bundle bundle) throws FhirLocalPersistingException {
        // TODO use fhirPersistenceService
        // should contain the Patient, the ServiceRequest, and the Task
        Bundle transactionResponseBundle = saveRemoteTaskAsLocalTask(sourceFhirClient, remoteTask, bundle,
                remoteStorePath);

        Task taskBasedOnRemoteTask = getLocalTaskBasedOnTask(remoteTask, remoteStorePath);
        if (taskBasedOnRemoteTask == null) {
            taskBasedOnRemoteTask = saveTaskBasedOnRemoteTask(sourceFhirClient, remoteTask, bundle, remoteStorePath);
        }

        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Task localTask = getTaskWithSameIdentifier(remoteTask, remoteStorePath);
        Bundle localBundle = localFhirClient.search()//
                .forResource(Task.class)//
                // TODO use include
                .include(Task.INCLUDE_PATIENT)//
                .include(Task.INCLUDE_BASED_ON)//
                .include(ServiceRequest.INCLUDE_PATIENT.asRecursive())//
                .where(Task.RES_ID.exactly().codes(localTask.getIdElement().getIdPart()))//
                .returnBundle(Bundle.class)//
                .execute();

        List<ServiceRequest> serviceRequestList = new ArrayList<>();
        Patient patient = null;

        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.isResource() && ResourceType.ServiceRequest.equals(entry.getResource().getResourceType())) {
                serviceRequestList.add((ServiceRequest) entry.getResource());
            } else if (entry.isResource() && ResourceType.Patient.equals(entry.getResource().getResourceType())) {
                patient = ((Patient) entry.getResource());
            }

        }
        if (patient == null) {
            LogEvent.logWarn(this.getClass().getName(), "processTaskImportOrder",
                    "no local patients found for Task " + localTask.getId());
        }
        if (serviceRequestList.size() == 0) {
            LogEvent.logWarn(this.getClass().getName(), "processTaskImportOrder",
                    "no local serviceRequests found for Task " + localTask.getId());
        }

        TaskResult taskResult = null;
//            if (localTask.getStatus() == null || !(localTask.getStatus().equals(TaskStatus.ACCEPTED)
//                    || localTask.getStatus().equals(TaskStatus.COMPLETED))) {
        if (localTask.getStatus() == null || localTask.getStatus().equals(TaskStatus.REQUESTED)) {
            Boolean taskOrderAcceptedFlag = false;
            for (ServiceRequest serviceRequest : serviceRequestList) {

                TaskWorker worker = new TaskWorker(remoteTask,
                        fhirContext.newJsonParser().encodeResourceToString(remoteTask), serviceRequest, patient);

                worker.setInterpreter(SpringContext.getBean(TaskInterpreter.class));
                worker.setExistanceChecker(SpringContext.getBean(DBOrderExistanceChecker.class));
                worker.setPersister(SpringContext.getBean(IOrderPersister.class));

                taskResult = worker.handleOrderRequest();
                if (taskResult == TaskResult.OK) {
                    taskOrderAcceptedFlag = true; // at least one order was accepted per Piotr 5/14/2020
                }
            }

            TaskStatus taskStatus = taskOrderAcceptedFlag ? TaskStatus.RECEIVED : TaskStatus.REJECTED;
            localTask.setStatus(taskStatus);
            if (remoteStoreUpdateStatus.isPresent() && remoteStoreUpdateStatus.get()) {
                LogEvent.logDebug(this.getClass().getName(), "beginTaskPath",
                        "updating remote status to " + taskStatus);
                remoteTask.setStatus(taskStatus);
                sourceFhirClient.update().resource(remoteTask).execute();
            }
            localFhirClient.update().resource(localTask).execute();
            taskBasedOnRemoteTask.setStatus(taskStatus);
            localFhirClient.update().resource(taskBasedOnRemoteTask).execute();
        }
    }

    private Task getLocalTaskBasedOnTask(Task remoteTask, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(Task.class)
                .where(Task.BASED_ON
                        .hasAnyOfIds(ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart()))
                .returnBundle(Bundle.class).execute();
        return (Task) localBundle.getEntryFirstRep().getResource();
    }

    private Task saveTaskBasedOnRemoteTask(IGenericClient fhirClient, Task remoteTask, Bundle bundle,
            String remoteStorePath) {
        Task taskBasedOnRemoteTask = new Task();
        taskBasedOnRemoteTask.setId(UUID.randomUUID().toString());
        Reference reference = new Reference();
        String referenceString = remoteStorePath;
        if (!referenceString.endsWith("/")) {
            referenceString = referenceString + "/";
        }
        referenceString = referenceString + ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart();
        reference.setReference(referenceString);
        taskBasedOnRemoteTask.addBasedOn(reference);

        MethodOutcome outcome = fhirUtil.getFhirClient(localFhirStorePath).update().resource(taskBasedOnRemoteTask)
                .execute();

        return (Task) outcome.getResource();
    }

    private Bundle saveRemoteTaskAsLocalTask(IGenericClient sourceFhirClient, Task remoteTask, Bundle bundle,
            String remoteStorePath) throws FhirLocalPersistingException {
        FhirOperations fhirOperations = new FhirOperations();
        TempIdGenerator idGenerator = new CountingTempIdGenerator();

        List<ServiceRequest> remoteServiceRequests = getBasedOnServiceRequestsFromServer(sourceFhirClient, remoteTask);
        List<Patient> remotePatients = new ArrayList<>();
        Patient remotePatientForTask = getForPatientFromServer(sourceFhirClient, remoteTask);
        if (remotePatientForTask == null) {
            remotePatients = getForPatientFromServer(sourceFhirClient, remoteServiceRequests);
        } else {
            remotePatients.add(remotePatientForTask);
        }

        Task localTask = getTaskWithSameIdentifier(remoteTask, remoteStorePath);
        if (localTask == null) {
            localTask = remoteTask.addIdentifier(createIdentifierToRemoteResource(remoteTask, remoteStorePath));
//            localTask.setStatus(TaskStatus.ACCEPTED);
            fhirOperations.createResources.put(idGenerator.getNextId(), localTask);
        } else {
            fhirOperations.updateResources.put(localTask.getIdElement().getIdPart(), localTask);
        }

//        if (localTask.hasEncounter()) {
//            replaceLocalReferenceWithAbsoluteReference(remoteStorePath, localTask.getEncounter());
//        }
//        if (localTask.hasOwner()) {
//            replaceLocalReferenceWithAbsoluteReference(remoteStorePath, localTask.getOwner());
//        }

        // ServiceRequests
//      List<ServiceRequest> basedOnServiceRequests = getBasedOnServiceRequestFromBundle(bundle, remoteTask);
        for (ServiceRequest remoteBasedOn : remoteServiceRequests) {
            Optional<ServiceRequest> localBasedOn = getServiceRequestWithSameIdentifier(remoteBasedOn, remoteStorePath);
            if (localBasedOn.isEmpty()) {
                fhirOperations.createResources.put(idGenerator.getNextId(),
                        remoteBasedOn.addIdentifier(createIdentifierToRemoteResource(remoteBasedOn, remoteStorePath)));
            } else {
                fhirOperations.updateResources.put(localBasedOn.get().getIdElement().getIdPart(), localBasedOn.get());
            }
        }

        // Patient
//      Patient forPatient = getForPatientFromBundle(bundle, remoteTask);
        for (Patient patient : remotePatients) {
            Optional<Patient> localForPatient = getPatientWithSameServiceIdentifier(patient, remoteStorePath);
            if (localForPatient.isEmpty()) {
//                localForPatient.addLink(new PatientLinkComponent().setType(LinkType.SEEALSO).setOther(patient));
                fhirOperations.createResources.put(idGenerator.getNextId(),
                        patient.addIdentifier(createIdentifierToRemoteResource(patient, remoteStorePath)));
            } else {
                // patient already exists so we should update the reference to ours
                fhirOperations.updateResources.put(localForPatient.get().getIdElement().getIdPart(),
                        localForPatient.get());
            }
        }

        LogEvent.logDebug(this.getClass().getName(), "",
                "creating local copies of the remote fhir objects relating to Task: "
                        + remoteTask.getIdElement().getIdPart());
        // Run the transaction
        return fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    private List<Patient> getForPatientFromServer(IGenericClient fhirClient, List<ServiceRequest> serviceRequests) {
        List<Patient> patients = new ArrayList<>();
        for (ServiceRequest serviceRequest : serviceRequests) {
            if (serviceRequest.getSubject() != null && serviceRequest.getSubject().getReference() != null) {
                patients.add(fhirClient.read().resource(Patient.class)
                        .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()).execute());
            }
        }
        return patients;
    }

    private Identifier createIdentifierToRemoteResource(IDomainResource remoteResource, String remoteStorePath) {
        Identifier identifier = new Identifier();
        identifier.setSystem(remoteStorePath);
        identifier.setType(new CodeableConcept()
                .addCoding(new Coding().setCode("externalId").setSystem(fhirConfig.getOeFhirSystem() + "/genIdType")));
        identifier.setValue(remoteResource.getIdElement().getIdPart());
        return identifier;
    }

    private Task getTaskWithSameIdentifier(Task remoteTask, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search()//
                .forResource(Task.class)//
                .where(Task.IDENTIFIER.exactly().codes(remoteTask.getIdElement().getIdPart()))//
                .returnBundle(Bundle.class).execute();
        return (Task) localBundle.getEntryFirstRep().getResource();
    }

    private Optional<ServiceRequest> getServiceRequestWithSameIdentifier(ServiceRequest basedOn,
            String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search()//
                .forResource(ServiceRequest.class)//
                .where(ServiceRequest.IDENTIFIER.exactly().codes(basedOn.getIdElement().getIdPart()))//
                .returnBundle(Bundle.class).execute();
        if (localBundle.hasEntry()) {
            return Optional.of((ServiceRequest) localBundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    private Optional<Patient> getPatientWithSameServiceIdentifier(Patient remotePatient, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);

        Bundle localBundle = localFhirClient.search()//
                .forResource(Patient.class)//
                .where(Patient.IDENTIFIER.exactly().codes(remotePatient.getIdElement().getIdPart()))//
                .returnBundle(Bundle.class).execute();
        if (localBundle.hasEntry()) {
            return Optional.of((Patient) localBundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    private List<ServiceRequest> getBasedOnServiceRequestsFromServer(IGenericClient fhirClient, Task remoteTask) {
        List<ServiceRequest> basedOn = new ArrayList<>();
        for (Reference basedOnElement : remoteTask.getBasedOn()) {
            basedOn.add(
                    fhirClient.read().resource(ServiceRequest.class)
                            .withId(basedOnElement.getReferenceElement().getIdPart()).execute());
        }
        return basedOn;
    }

    private Patient getForPatientFromServer(IGenericClient fhirClient, Task remoteTask) {
        Patient forPatient = null;
        if (!(remoteTask.getFor() == null || remoteTask.getFor().getReference() == null)) {
            forPatient = fhirClient.read().resource(Patient.class)
                    .withId(remoteTask.getFor().getReferenceElement().getIdPart())
                .execute();
        }

        if (forPatient == null ) {
            LogEvent.logWarn(this.getClass().getName(), "getForPatientFromServer",
                    "remoteTask doesn't reference a patient, or referenced patient returned null");
        }
        return forPatient;
    }

    public class OriginalReferralObjects {
        public Task task;
        public ServiceRequest serviceRequest;
    }

    public class ReferralResultsImportObjects {
        public OriginalReferralObjects originalReferralObjects = new OriginalReferralObjects();
        public ServiceRequest serviceRequest;
        public DiagnosticReport diagnosticReport;
        public List<Observation> observations = new ArrayList<>();
    }
}
