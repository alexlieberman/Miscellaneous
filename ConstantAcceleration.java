import java.math.BigInteger;

// If the speed of light was a fallacy
// If constant acceleration/deceleration was possible
// How long would it take to get to a destination based on meters/second acceleration
// Assuming you had to decelerate at the halfway point
public class ConstantAcceleration {

	public static void main(String[] args) {

		int metersPerSecond = 10;
		
		travelTo("41343392165178100", "Closest Solar System", metersPerSecond);
		travelTo("1548188928000", "Saturn", metersPerSecond);
		travelTo("778922496000", "Jupiter", metersPerSecond);
		travelTo("225000000000", "Mars", metersPerSecond);
		
	}
	
	public static void travelTo(String distanceMeters, String planet, Integer accelerationRate) {
		
		distanceMeters = distanceMeters.replaceAll(",","");
		
		BigInteger second = new BigInteger("1");
		BigInteger two = new BigInteger("2");
		BigInteger sixty = new BigInteger("60");
		BigInteger twentyfour = new BigInteger("24");
		BigInteger thirty = new BigInteger("30");
		BigInteger tripMeters = new BigInteger(distanceMeters);
		BigInteger halfway = tripMeters.divide(two);
		
		BigInteger seconds = new BigInteger("0");
		BigInteger acceleration = new BigInteger(String.valueOf(accelerationRate));
		BigInteger currentAcceleration = new BigInteger("0");
		BigInteger traveled = new BigInteger("0");
	
		while (traveled.compareTo(halfway) <= 0) {
			
			currentAcceleration = currentAcceleration.add(acceleration);
			traveled = traveled.add(currentAcceleration);
			seconds = seconds.add(second);
		}
		
		seconds = seconds.multiply(two);
		seconds = seconds.divide(sixty); //minutes
		seconds = seconds.divide(sixty);
		seconds = seconds.divide(twentyfour); //days
		System.out.println(planet + " : " + seconds + " days at maximum speed of " + currentAcceleration + " m/s");
		
	}
	
}
