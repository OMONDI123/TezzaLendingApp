package co.ke.tezza.loanapp.model;

import java.util.List;

public class LocationResponse {
    private List<ResultItem> results;
    private String status;

    // getters and setters
    public List<ResultItem> getResults() { return results; }
    public void setResults(List<ResultItem> results) { this.results = results; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class ResultItem {
    private List<AddressComponent> address_components;
    private Geometry geometry;

    public List<AddressComponent> getAddress_components() { return address_components; }
    public void setAddress_components(List<AddressComponent> address_components) { this.address_components = address_components; }

    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry geometry) { this.geometry = geometry; }
}

class AddressComponent {
    private String long_name;
    private String short_name;
    private List<String> types;

    public String getLong_name() { return long_name; }
    public void setLong_name(String long_name) { this.long_name = long_name; }

    public String getShort_name() { return short_name; }
    public void setShort_name(String short_name) { this.short_name = short_name; }

    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
}

class Geometry {
    private Location location;

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
}

class Location {
    private double lat;
    private double lng;

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
