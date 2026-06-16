package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReceiverCategory {
	
	ALL_ACCOUNTS("ALL_ACCOUNTS", "All Accounts"),
	
	ALL_MEMBERS("ALL_MEMBERS", "All Members"),
	ACTIVE_MEMBERS("ACTIVE_MEMBERS", "Active Members"),
	INACTIVE_MEMBERS("INACTIVE_MEMBERS", "Inactive Members"),
	
	EXPIRED_ACCOUNTS("EXPIRED_ACCOUNTS", "Expired Accounts"),
	SOON_EXPIRING_ACCOUNTS("SOON_EXPIRING_ACCOUNTS", "Soon Expiring Accounts"),
	
	BORROWERS_WITH_BALANCES("BORROWERS_WITH_BALANCES", "Borrowers with Balances Only"),
	BORROWERS_WITH_NO_BALANCE("BORROWERS_WITH_NO_BALANCE", "Borrowers with No Balance"),
	ALL_BORROWERS("ALL_BORROWERS", "All Borrowers"),
	
	SPECIFIC_ACCOUNTS("SPECIFIC_ACCOUNTS", "Specific/Individual Accounts"),
	
	ALL_ACCOUNTS_TYPES("ALL_ACCOUNTS_TYPES", "All Account Types"),
	SPECIFIC_ACCOUNT_TYPE("SPECIFIC_ACCOUNT_TYPE", "Specific Account Type"),
	ALL("ALL", "All"),
	SPECIFIC_OR_INDIVIDUAL_BORROWER("SPECIFIC_OR_INDIVIDUAL_BORROWER", "Individual/Specific Borrower");

	private final String value;
	private final String description;

	ReceiverCategory(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	@JsonCreator
	public static ReceiverCategory forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (ReceiverCategory base : ReceiverCategory.values()) {
			if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
				return base;
			}
		}
		return null;
	}
}