package spring.mine.validation.constraintvalidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import spring.mine.validation.annotations.ValidName;
import spring.service.siteinformation.SiteInformationService;
import spring.util.SpringContext;
import us.mn.state.health.lims.siteinformation.valueholder.SiteInformation;

public class NameValidator implements ConstraintValidator<ValidName, String> {

	public enum NameType {
		USERNAME, FIRST_NAME, LAST_NAME, FULL_NAME
	}

	private final String FIRST_NAME_REGEX;
	private final String LAST_NAME_REGEX;
	private final String FULL_NAME_REGEX;
	private final String USERNAME_REGEX;

	private NameType nameType;

	public NameValidator() {
		SiteInformationService siteInformationService = SpringContext.getBean(SiteInformationService.class);

		String firstNameRegex = siteInformationService.getMatch("name", "firstNameCharset").orElse(new SiteInformation()).getValue();
		firstNameRegex = escapeRegexChars(firstNameRegex);
		FIRST_NAME_REGEX = "(?i)^[" + firstNameRegex + "]*$";

		String lastNameRegex = siteInformationService.getMatch("name", "lastNameCharset").orElse(new SiteInformation()).getValue();
		lastNameRegex = escapeRegexChars(lastNameRegex);
		LAST_NAME_REGEX = "(?i)^[" + lastNameRegex + "]*$";

		FULL_NAME_REGEX = "(?i)^[" + firstNameRegex + "]*([ ]*[" + lastNameRegex + "])?$";

		String usernameRegex = siteInformationService.getMatch("name", "userNameCharset").orElse(new SiteInformation()).getValue();
		usernameRegex = escapeRegexChars(usernameRegex);
		USERNAME_REGEX = "(?i)^[" + usernameRegex + "]*$";
	}

	private String escapeRegexChars(String regex) {
		// TODO Auto-generated method stub
		return regex;
	}

	@Override
	public void initialize(ValidName constraint) {
		nameType = constraint.nameType();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
			return true;
		}

		switch (nameType) {
		case FIRST_NAME:
			return value.matches(FIRST_NAME_REGEX);
		case LAST_NAME:
			return value.matches(LAST_NAME_REGEX);
		case FULL_NAME:
			return value.matches(FULL_NAME_REGEX);
		case USERNAME:
			return value.matches(USERNAME_REGEX);

		}
		if (nameType == NameType.FIRST_NAME) {
		} else if (nameType == NameType.LAST_NAME) {
			return value.matches(LAST_NAME_REGEX);
		} else if (nameType == NameType.FULL_NAME) {
			return value.matches(FULL_NAME_REGEX);
		}

		return false;

	}
}