package spring.mine.samplebatchentry.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.beanutils.PropertyUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.controller.BaseController;
import spring.mine.common.validator.BaseErrors;
import spring.mine.sample.form.SamplePatientEntryForm;
import spring.mine.sample.validator.SamplePatientEntryFormValidator;
import spring.mine.samplebatchentry.form.SampleBatchEntryForm;
import spring.mine.samplebatchentry.validator.SampleBatchEntryFormValidator;
import spring.service.organization.OrganizationService;
import spring.service.sample.PatientManagementUpdate;
import spring.service.sample.SamplePatientEntryService;
import spring.service.test.TestService;
import spring.service.test.TestServiceImpl;
import spring.service.typeofsample.TypeOfSampleService;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.services.SampleOrderService;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.validator.GenericValidator;
import us.mn.state.health.lims.organization.valueholder.Organization;
import us.mn.state.health.lims.patient.action.IPatientUpdate;
import us.mn.state.health.lims.patient.action.IPatientUpdate.PatientUpdateStatus;
import us.mn.state.health.lims.patient.action.bean.PatientManagementInfo;
import us.mn.state.health.lims.patient.action.bean.PatientSearch;
import us.mn.state.health.lims.sample.action.util.SamplePatientUpdateData;
import us.mn.state.health.lims.sample.bean.SampleOrderItem;

@Controller
public class SampleBatchEntryController extends BaseController {

	@Autowired
	SampleBatchEntryFormValidator formValidator;

	@Autowired
	TestService testService;
	@Autowired
	TypeOfSampleService typeOfSampleService;
	@Autowired
	OrganizationService organizationService;

	@Autowired
	SamplePatientEntryFormValidator entryFormValidator;

	@Autowired
	private SamplePatientEntryService samplePatientService;
	@Autowired
	PatientManagementUpdate patientUpdate;

	@RequestMapping(value = { "/SampleBatchEntry" }, method = RequestMethod.POST)
	public ModelAndView showSampleBatchEntry(HttpServletRequest request,
			@ModelAttribute("form") @Valid SampleBatchEntryForm form, BindingResult result) throws DocumentException {
		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL, form);
		}

		String sampleXML = form.getSampleXML();
		SampleOrderService sampleOrderService = new SampleOrderService();
		SampleOrderItem soi = sampleOrderService.getSampleOrderItem();
		// preserve fields that are already in form in refreshed object
		soi.setReceivedTime(form.getSampleOrderItems().getReceivedTime());
		soi.setReceivedDateForDisplay(form.getSampleOrderItems().getReceivedDateForDisplay());
		soi.setNewRequesterName(form.getSampleOrderItems().getNewRequesterName());
		soi.setReferringSiteId(form.getFacilityID());
		form.setSampleOrderItems(soi);

		form.setLocalDBOnly(ConfigurationProperties.getInstance()
				.getPropertyValueLowerCase(Property.UseExternalPatientInfo).equals("false"));
		/*
		 * errors = validate(request); if (errors.hasErrors()) { saveErrors(errors,
		 * form); request.setAttribute(IActionConstants.FWD_SUCCESS, false); forward =
		 * FWD_FAIL; return findForward(forward, form); }
		 */

		// get summary of tests selected to place in common fields section
		Document sampleDom = DocumentHelper.parseText(sampleXML);
		Element sampleItem = sampleDom.getRootElement().element("sample");
		String testIDs = sampleItem.attributeValue("tests");
		StringTokenizer tokenizer = new StringTokenizer(testIDs, ",");
		StringBuilder sBuilder = new StringBuilder();
		String seperator = "";
		while (tokenizer.hasMoreTokens()) {
			sBuilder.append(seperator);
			sBuilder.append(TestServiceImpl.getUserLocalizedTestName(testService.get(tokenizer.nextToken().trim())));
			seperator = "<br>";
		}
		String sampleType = typeOfSampleService.get(sampleItem.attributeValue("sampleID")).getLocalAbbreviation();
		String testNames = sBuilder.toString();
		request.setAttribute("sampleType", sampleType);
		request.setAttribute("testNames", testNames);

		// get facility name from id
		String facilityName = "";
		if (!StringUtil.isNullorNill(form.getFacilityID())) {
			Organization organization = organizationService.get(form.getFacilityID());
			facilityName = organization.getOrganizationName();
		} else if (!StringUtil.isNullorNill(form.getSampleOrderItems().getNewRequesterName())) {
			facilityName = form.getSampleOrderItems().getNewRequesterName();
		}
		request.setAttribute("facilityName", facilityName);
		form.setPatientSearch(new PatientSearch());

		return findForward(form.getMethod(), form);
	}

	@RequestMapping(value = "/SamplePatientEntryBatch", method = RequestMethod.POST)
	public @ResponseBody ModelAndView showSamplePatientEntrySave(HttpServletRequest request,
			@ModelAttribute("form") @Validated(SamplePatientEntryForm.SamplePatientEntryBatch.class) SamplePatientEntryForm form,
			BindingResult result, RedirectAttributes redirectAttributes)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		entryFormValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}
		SamplePatientUpdateData updateData = new SamplePatientUpdateData(getSysUserId(request));

		PatientManagementInfo patientInfo = (PatientManagementInfo) PropertyUtils.getProperty(form,
				"patientProperties");
		SampleOrderItem sampleOrder = (SampleOrderItem) PropertyUtils.getProperty(form, "sampleOrderItems");

		boolean trackPayments = ConfigurationProperties.getInstance()
				.isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");

		String receivedDateForDisplay = sampleOrder.getReceivedDateForDisplay();

		if (!GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
			receivedDateForDisplay += " " + sampleOrder.getReceivedTime();
		} else {
			receivedDateForDisplay += " 00:00";
		}

		updateData.setCollectionDateFromRecieveDateIfNeeded(receivedDateForDisplay);
		updateData.initializeRequester(sampleOrder);

		patientUpdate.setSysUserIdFromRequest(request);
		testAndInitializePatientForSaving(request, patientInfo, patientUpdate, updateData);

		updateData.setAccessionNumber(sampleOrder.getLabNo());
		updateData.initProvider(sampleOrder);
		updateData.initSampleData(form.getSampleXML(), receivedDateForDisplay, trackPayments, sampleOrder);
		updateData.validateSample(result);

		if (result.hasErrors()) {
			saveErrors(result);
			// setSuccessFlag(request, true);
			return findForward(FWD_FAIL_INSERT, form);
		}

		try {
			samplePatientService.persistData(updateData, patientUpdate, patientInfo, form, request);
		} catch (LIMSRuntimeException lre) {
			// ActionError error;
			if (lre.getException() instanceof StaleObjectStateException) {
				// error = new ActionError("errors.OptimisticLockException", null, null);
				result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
			} else {
				lre.printStackTrace();
				// error = new ActionError("errors.UpdateException", null, null);
				result.reject("errors.UpdateException", "errors.UpdateException");
			}
			System.out.println(result);

			// errors.add(ActionMessages.GLOBAL_MESSAGE, error);
			saveErrors(result);
			request.setAttribute(ALLOW_EDITS_KEY, "false");
			return findForward(FWD_FAIL_INSERT, form);

		}

		redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
		return findForward(FWD_SUCCESS_INSERT, form);
	}

	private void testAndInitializePatientForSaving(HttpServletRequest request, PatientManagementInfo patientInfo,
			IPatientUpdate patientUpdate, SamplePatientUpdateData updateData)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		patientUpdate.setPatientUpdateStatus(patientInfo);
		updateData.setSavePatient(patientUpdate.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION);

		if (updateData.isSavePatient()) {
			updateData.setPatientErrors(patientUpdate.preparePatientData(request, patientInfo));
		} else {
			updateData.setPatientErrors(new BaseErrors());
		}
	}

	@Override
	protected String findLocalForward(String forward) {
		if ("On Demand".equals(forward)) {
			return "sampleBatchEntryOnDemandDefinition";
		} else if ("Pre-Printed".equals(forward)) {
			return "sampleBatchEntryPrePrintedDefinition";
		} else if (FWD_FAIL.equals(forward)) {
			return "sampleBatchEntrySetupDefinition";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "sampleBatchEntrySetupDefinition";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "redirect:/SamplePatientEntry.do";
		} else {
			return "redirect:/SampleBatchEntrySetup.do";
		}
	}

	@Override
	protected String getPageTitleKey() {
		return "sample.batchentry.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "sample.batchentry.title";
	}
}
