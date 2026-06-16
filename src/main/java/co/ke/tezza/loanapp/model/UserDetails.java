/**
 * 
 */
package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author austine
 *
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails {
    private Long id;
    private String firstName;
    private String fullName;
    private String lastName;
    private String password;
    private String phoneNo;
    private String email;
    private String dateOfBirth;
    private List<Long> userRoleIds = new ArrayList<>();
    private boolean isActive;
    private String recapcha;
    private String referralCode;
    private String gender;

    // 🌍 Location fields (from IP API)
    private String ip;
    private String network;
    private String version;
    private String city;
    private String region;
    private String regionCode;
    private String country;
    private String countryName;
    private String countryCode;
    private String countryCodeIso3;
    private String countryCapital;
    private String countryTld;
    private String continentCode;
    private boolean inEu;
    private String postal;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private String utcOffset;
    private String countryCallingCode;
    private String currency;
    private String currencyName;
    private String languages;
    private Double countryArea;
    private Long countryPopulation;
    private String asn;
    private String org;
    private Double currentLat;
    private Double currentLong;
}
	
	


