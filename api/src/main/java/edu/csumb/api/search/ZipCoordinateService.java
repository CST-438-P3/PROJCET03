package edu.csumb.api.search;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Temporary ZIP lookup. Replace the map with a geocoding provider later. */
@Component
public class ZipCoordinateService {
    private final Map<String, Coordinate> coordinates = Map.of(
            "93933", new Coordinate(36.7068, -121.6555),
            "93906", new Coordinate(36.6777, -121.6550),
            "95060", new Coordinate(37.0511, -122.0839));

    public Coordinate lookup(String zip) {
        Coordinate coordinate = coordinates.get(zip);
        if (coordinate == null) {
            throw new IllegalArgumentException("We couldn't find that ZIP code");
        }
        return coordinate;
    }

    public record Coordinate(double latitude, double longitude) { }
}
