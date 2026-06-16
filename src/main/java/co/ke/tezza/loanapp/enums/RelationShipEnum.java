package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RelationShipEnum {
    SPOUSE("SPOUSE", "Spouse"),
    FATHER("FATHER", "Father"),
    MOTHER("MOTHER", "Mother"),
    BROTHER("BROTHER", "Brother"),
    SISTER("SISTER", "Sister"),
    FRIEND("FRIEND", "Friend"),
    UNCLE("UNCLE", "Uncle"),
    AUNT("AUNT", "Aunt"),
    GUARDIAN("GUARDIAN", "Guardian"),
    DIR("DIR", "Director"),
    CEO("CEO", "Chief Executive Officer"),
    MGR("MGR", "Manager"),
    CFO("CFO", "Chief Financial Officer"),
    COO("COO", "Chief Operating Officer"),
    SEC("SEC", "Company Secretary"),
    CHAIR("CHAIR", "Chairperson"),
    TRUSTEE("TRUSTEE", "Trustee"),
    AUDITOR("AUDITOR", "Auditor"),
    LEGAL("LEGAL", "Legal Representative"),
    EMPLOYEE("EMPLOYEE", "Employee"),
    ADMIN("ADMIN", "Organisation Administrator"),
    OTHER("OTHER", "Other");

    private final String value;
    private final String description;

    RelationShipEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static RelationShipEnum fromValue(String value) {
        for (RelationShipEnum rel : RelationShipEnum.values()) {
            if (rel.value.equalsIgnoreCase(value) || rel.description.equalsIgnoreCase(value)) {
                return rel;
            }
        }
        throw new IllegalArgumentException("Unknown relationship value: " + value);
    }

    @JsonCreator
    public static RelationShipEnum forValues(@JsonProperty("value") String value,
                                             @JsonProperty("description") String description) {
        for (RelationShipEnum rel : RelationShipEnum.values()) {
            if (rel.value.equalsIgnoreCase(value) || rel.description.equalsIgnoreCase(value)) {
                return rel;
            }
        }
        return null;
    }
}
