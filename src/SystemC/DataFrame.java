package SystemC; 

public class DataFrame {
	public long timestamp = 0;
	public double velocity = 0;
	public double altitude = 0;
	public double pressure = 0;
	public double temperature = 0;
	public double attitude = 0; 
	
	@Override
	public String toString() {
		return "DataFrame [timestamp=" + timestamp + ", velocity=" + velocity
				+ ", altitude=" + altitude + ", pressure=" + pressure
				+ ", temperature=" + temperature + ", attitude=" + attitude
				+ "]";
	}
}
