/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package us.mn.state.health.lims.sample.bean;

import java.io.Serializable;
import java.util.Collection;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.SafeHtml;

import spring.mine.common.validator.ValidationHelper;
import spring.mine.sample.form.SampleEditForm;
import spring.mine.sample.form.SamplePatientEntryForm;
import spring.mine.sample.form.SamplePatientEntryForm.SamplePatientEntryBatch;
import spring.mine.samplebatchentry.form.SampleBatchEntryForm;
import spring.mine.validation.annotations.OptionalNotBlank;
import spring.mine.validation.annotations.ValidAccessionNumber;
import spring.mine.validation.annotations.ValidDate;
import spring.mine.validation.annotations.ValidName;
import spring.mine.validation.annotations.ValidTime;
import spring.mine.validation.constraintvalidator.NameValidator.NameType;
import us.mn.state.health.lims.common.formfields.FormFields.Field;
import us.mn.state.health.lims.common.util.validator.CustomDateValidator.DateRelation;

public class SampleOrderItem implements Serializable {
	private static final long serialVersionUID = 1L;

	@SafeHtml(groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
			SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String newRequesterName;

	// for display
	private Collection orderTypes;

	@Pattern(regexp = ValidationHelper.ID_REGEX)
	private String orderType;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class })
	private String externalOrderNumber;

	@NotBlank(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class })
	@ValidAccessionNumber(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String labNo;

	@OptionalNotBlank(formFields = { Field.SampleEntryUseRequestDate }, groups = {
			SamplePatientEntryForm.SamplePatientEntry.class, SampleEditForm.SampleEdit.class })
	@ValidDate(relative = DateRelation.PAST, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String requestDate;

	@NotBlank(groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
			SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	@ValidDate(relative = DateRelation.PAST, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
			SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String receivedDateForDisplay;

	@ValidTime(groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
			SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String receivedTime;

	@ValidDate(relative = DateRelation.FUTURE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String nextVisitDate;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String requesterSampleID;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String referringPatientNumber;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String referringSiteId;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String referringSiteCode;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String referringSiteName;

	// for display
	private Collection referringSiteList;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class })
	private String providerId;

	@ValidName(nameType = NameType.FIRST_NAME, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String providerFirstName;

	@ValidName(nameType = NameType.LAST_NAME, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String providerLastName;

	@Pattern(regexp = ValidationHelper.PHONE_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String providerWorkPhone;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String providerFax;

	@Email(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String providerEmail;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String facilityAddressStreet;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String facilityAddressCommune;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String facilityPhone;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String facilityFax;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String paymentOptionSelection;

	// for display
	private Collection paymentOptions;

	@NotNull(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private Boolean modified = false;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class })
	private String sampleId;

	private boolean readOnly = false;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String billingReferenceNumber;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String testLocationCode;

	@SafeHtml(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
			SampleEditForm.SampleEdit.class })
	private String otherLocationCode;

	// for display
	private Collection testLocationCodeList;

	@Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
			SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
	private String program;

	// for display
	private Collection programList;

	public String getNewRequesterName() {
		return newRequesterName;
	}

	public void setNewRequesterName(String newRequesterName) {
		this.newRequesterName = newRequesterName;
	}

	public Collection getOrderTypes() {
		return orderTypes;
	}

	public void setOrderTypes(Collection orderTypes) {
		this.orderTypes = orderTypes;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getExternalOrderNumber() {
		return externalOrderNumber;
	}

	public void setExternalOrderNumber(String externalOrderNumber) {
		this.externalOrderNumber = externalOrderNumber;
	}

	public String getLabNo() {
		return labNo;
	}

	public void setLabNo(String labNo) {
		this.labNo = labNo;
	}

	public String getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
	}

	public String getReceivedDateForDisplay() {
		return receivedDateForDisplay;
	}

	public void setReceivedDateForDisplay(String receivedDateForDisplay) {
		this.receivedDateForDisplay = receivedDateForDisplay;
	}

	public String getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(String receivedTime) {
		this.receivedTime = receivedTime;
	}

	public String getNextVisitDate() {
		return nextVisitDate;
	}

	public void setNextVisitDate(String nextVisitDate) {
		this.nextVisitDate = nextVisitDate;
	}

	public String getRequesterSampleID() {
		return requesterSampleID;
	}

	public void setRequesterSampleID(String requesterSampleID) {
		this.requesterSampleID = requesterSampleID;
	}

	public String getReferringPatientNumber() {
		return referringPatientNumber;
	}

	public void setReferringPatientNumber(String referringPatientNumber) {
		this.referringPatientNumber = referringPatientNumber;
	}

	public String getReferringSiteId() {
		return referringSiteId;
	}

	public void setReferringSiteId(String referringSiteId) {
		this.referringSiteId = referringSiteId;
	}

	public String getReferringSiteCode() {
		return referringSiteCode;
	}

	public void setReferringSiteCode(String referringSiteCode) {
		this.referringSiteCode = referringSiteCode;
	}

	public String getReferringSiteName() {
		return referringSiteName;
	}

	public void setReferringSiteName(String referringSiteName) {
		this.referringSiteName = referringSiteName;
	}

	public Collection getReferringSiteList() {
		return referringSiteList;
	}

	public void setReferringSiteList(Collection referringSiteList) {
		this.referringSiteList = referringSiteList;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getProviderFirstName() {
		return providerFirstName;
	}

	public void setProviderFirstName(String providerFirstName) {
		this.providerFirstName = providerFirstName;
	}

	public String getProviderLastName() {
		return providerLastName;
	}

	public void setProviderLastName(String providerLastName) {
		this.providerLastName = providerLastName;
	}

	public String getProviderWorkPhone() {
		return providerWorkPhone;
	}

	public void setProviderWorkPhone(String providerWorkPhone) {
		this.providerWorkPhone = providerWorkPhone;
	}

	public String getProviderFax() {
		return providerFax;
	}

	public void setProviderFax(String providerFax) {
		this.providerFax = providerFax;
	}

	public String getProviderEmail() {
		return providerEmail;
	}

	public void setProviderEmail(String providerEmail) {
		this.providerEmail = providerEmail;
	}

	public String getFacilityAddressStreet() {
		return facilityAddressStreet;
	}

	public void setFacilityAddressStreet(String facilityAddressStreet) {
		this.facilityAddressStreet = facilityAddressStreet;
	}

	public String getFacilityAddressCommune() {
		return facilityAddressCommune;
	}

	public void setFacilityAddressCommune(String facilityAddressCommune) {
		this.facilityAddressCommune = facilityAddressCommune;
	}

	public String getFacilityPhone() {
		return facilityPhone;
	}

	public void setFacilityPhone(String facilityPhone) {
		this.facilityPhone = facilityPhone;
	}

	public String getFacilityFax() {
		return facilityFax;
	}

	public void setFacilityFax(String facilityFax) {
		this.facilityFax = facilityFax;
	}

	public String getPaymentOptionSelection() {
		return paymentOptionSelection;
	}

	public void setPaymentOptionSelection(String paymentOptionSelection) {
		this.paymentOptionSelection = paymentOptionSelection;
	}

	public Collection getPaymentOptions() {
		return paymentOptions;
	}

	public void setPaymentOptions(Collection paymentOptions) {
		this.paymentOptions = paymentOptions;
	}

	public String getOtherLocationCode() {
		return otherLocationCode;
	}

	public void setOtherLocationCode(String otherLocationCode) {
		this.otherLocationCode = otherLocationCode;
	}

	public Boolean getModified() {
		return modified;
	}

	public void setModified(Boolean modified) {
		this.modified = modified;
	}

	public String getSampleId() {
		return sampleId;
	}

	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public String getBillingReferenceNumber() {
		return billingReferenceNumber;
	}

	public void setBillingReferenceNumber(String billingReferenceNumber) {
		this.billingReferenceNumber = billingReferenceNumber;
	}

	public String getTestLocationCode() {
		return testLocationCode;
	}

	public void setTestLocationCode(String testLocationCode) {
		this.testLocationCode = testLocationCode;
	}

	public Collection getTestLocationCodeList() {
		return testLocationCodeList;
	}

	public void setTestLocationCodeList(Collection testLocationCodeList) {
		this.testLocationCodeList = testLocationCodeList;
	}

	public String getProgram() {
		return program;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	public Collection getProgramList() {
		return programList;
	}

	public void setProgramList(Collection programList) {
		this.programList = programList;
	}
}