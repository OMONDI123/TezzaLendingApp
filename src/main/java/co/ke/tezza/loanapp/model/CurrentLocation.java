package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentLocation {
	private String currentCountry;
    private String currentCounty;
    private String currentSubCounty;
    private String currentLocality;
    private String currentNearestCity;
    private double currentLat;
    private double currentLng;
    private String currentLocationId;

    
}

