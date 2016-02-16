public class LatLongConvert {
	//perform test
    public final static double AVERAGE_RADIUS_OF_EARTH = 6371000;
	
    public static double[] calculateDistance(Double[] coordinate) {
    return new double[]{calculate(coordinate[0], coordinate[1], 0, coordinate[1]), calculate(coordinate[0], coordinate[1], coordinate[0], 0)};
	}

    private static double calculate(double userLat, double userLng, double venueLat, double venueLng) {

    double latDistance = Math.toRadians(userLat - venueLat);
    double lngDistance = Math.toRadians(userLng - venueLng);

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
      + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
      * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return AVERAGE_RADIUS_OF_EARTH * c;
}

	public static void main(String[] args) {
		double[] arr =calculateDistance(new Double[]{Double.parseDouble(args[0]), Double.parseDouble(args[1])});
		System.out.println(arr[0]+ " " + arr[1]);
	}
} 