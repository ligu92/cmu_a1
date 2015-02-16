package SystemC;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AltitudeFilter extends DataFrameFilterFramework {
	private String fileName = null;
	
	public AltitudeFilter(String fileName)
	{
		this.fileName = fileName;
	}
/*	public void run()
    {
		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(this.fileName));
			// read data frame from the first Stream 
			DataFrame df = readDataFrame(0);
			
			while (df != null ) {
				if (df.altitude < 10000) {
					out.writeInt(0);	
					out.writeLong(df.timestamp);
					
					// write velocity to OutputPort
					out.writeInt(1);
					out.writeLong(Double.doubleToLongBits(df.velocity));
					
					// write altitude to OutputPort
					out.writeInt(2);
					out.writeLong(Double.doubleToLongBits(df.altitude));
					
					// write pressure to OutputPort
					out.writeInt(3);
					out.writeLong(Double.doubleToLongBits(df.pressure));
					
					// write temperature to OutputPort
					out.writeInt(4);
					out.writeLong(Double.doubleToLongBits(df.temperature));
		
					// write attitude to OutputPort
					out.writeInt(5);
					out.writeLong(Double.doubleToLongBits(df.attitude));
				} else {
					try {
						writeDataFrame(df);
					} catch (EndOfStreamException e) {
						e.printStackTrace();
					}
					
				}
				df = readDataFrame(0);
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // run
*/
	public void run()
    {
		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy MM dd::hh:mm:ss:SSS");
		try {
			PrintStream out = new PrintStream(new FileOutputStream(this.fileName));
			// read data frame from the first Stream 
			DataFrame df = readDataFrame(0);
			
			while (df != null ) {
				if (df.altitude < 10000) {
					TimeStamp.setTimeInMillis(df.timestamp);
					out.println(TimeStampFormat.format(TimeStamp.getTime()) +"\t" + df.velocity + "\t" + df.altitude + "\t" + df.pressure + "\t" + df.temperature + "\t" + df.attitude);
				} else {
					try {
						writeDataFrame(df);
					} catch (EndOfStreamException e) {
						e.printStackTrace();
					}
					
				}
				df = readDataFrame(0);
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // run
}
