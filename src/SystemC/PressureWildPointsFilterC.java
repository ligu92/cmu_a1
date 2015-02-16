/******************************************************************************************************************
* File:MergeFilterC.java
* Course: 17655
* Project: Assignment 1
* Copyright: Copyright (c) 2003 Carnegie Mellon University
* Versions:
*	1.0 November 2008 - Initial rewrite of original assignment 1.
*
* Description:
*
* This class process wild points in the pressure, wild points encountered in the stream, 
* extrapolate a replacement value by using the last known valid measurement 
* and the next valid measurement in the stream
*
*
******************************************************************************************************************/
package SystemC;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PressureWildPointsFilterC extends DataFrameFilterFrameworkC {
	private String fileName = null;
	
	public PressureWildPointsFilterC(String fileName)
	{
		this.fileName = fileName;
	}
	public void run()
    {
		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy MM dd::hh:mm:ss:SSS");
		try {
			PrintStream out = new PrintStream(new FileOutputStream(this.fileName));
			// read data frame from the first Stream 
			DataFrameC df = readDataFrame(0);
			List<DataFrameC> dataFrames = new ArrayList<DataFrameC>();
			
			double lastValidPressure = 0;
			double nextValidPressure = 0;
			int lastValidPressureIndex = -1;
			
			while (df != null ) {
				dataFrames.add(df);
				df = readDataFrame(0);
			}
			
			DataFrameC dfNext = null;
			// find the first valid point at the beginning of the stream
			for (int i = 0; i < dataFrames.size() - 1; i++) {
				df = dataFrames.get(i);
				dfNext = dataFrames.get(i + 1);
				if (df.pressure >= 0){
					lastValidPressureIndex = i;
					lastValidPressure = df.pressure;
					break;
				}
			}
			
			// processing wild points at the beginning of the stream
			for (int i = 0; i < lastValidPressureIndex; i++) {
				df = dataFrames.get(i);
				// write wild pressure to out file
				TimeStamp.setTimeInMillis(df.timestamp);
				out.println(TimeStampFormat.format(TimeStamp.getTime()) +"\t" + df.pressure);
				
				// replace the wild value
				df.pressure = lastValidPressure;
				try {
					writeDataFrame(df);
				} catch (EndOfStreamException e) {
					e.printStackTrace();
				}
			}
		
			if (lastValidPressureIndex > 0) {
				// write the first valid value
				try {
					writeDataFrame(dataFrames.get(lastValidPressureIndex));
				} catch (EndOfStreamException e) {
					e.printStackTrace();
				}
			}
			for (int i = lastValidPressureIndex + 1; i < dataFrames.size(); i++) {
				df = dataFrames.get(i);
				if (df.pressure < 0 || Math.abs(df.pressure - lastValidPressure) > 10) {
					continue;
				}
				
				// process invalid values before i
				for(int j = lastValidPressureIndex + 1; j < i; j++) {
					DataFrameC dfTmp = dataFrames.get(j);
					// write wild pressure to out file
					TimeStamp.setTimeInMillis(dfTmp.timestamp);
					out.println(TimeStampFormat.format(TimeStamp.getTime()) +"\t" + dfTmp.pressure);
					dfTmp.pressure = (lastValidPressure + df.pressure) / 2;
					try {
						writeDataFrame(dfTmp);
					} catch (EndOfStreamException e) {
						e.printStackTrace();
					}
				}
				lastValidPressure = df.pressure;
				lastValidPressureIndex = i;
				try {
					writeDataFrame(df);
				} catch (EndOfStreamException e) {
					e.printStackTrace();
				}
			}
			// processing wild points at the end of the stream
			for (int i = lastValidPressureIndex + 1; i < dataFrames.size(); i++) {
				df = dataFrames.get(i);
				// write wild pressure to out file
				TimeStamp.setTimeInMillis(df.timestamp);
				out.println(TimeStampFormat.format(TimeStamp.getTime()) +"\t" + df.pressure);
				df.pressure = (lastValidPressure + df.pressure) / 2;
				try {
					writeDataFrame(df);
				} catch (EndOfStreamException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // run

}
