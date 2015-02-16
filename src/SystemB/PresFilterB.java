package SystemB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import SystemB.FilterFramework.EndOfStreamException;


/**
 * This is a filter which gets input from the splitter having the time stamp and pressure data
 * It recognises wildpoint and do the extension part of it
 * The entire output is given to the meger
 * @author sdadi, ligu
 *
 */
public class PresFilterB extends FilterFramework {

	/**
	 * Overload the super class constructor
	 * @param codesSet
	 */
	public PresFilterB(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	/**
	 * Starts off the filter operation
	 */
	public void run() {

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		//Bytes arrays for buffering
		byte[] id_bytes;
		byte[] pressureType= new byte[4];
		byte[] data_bytes;
		
		int byteswritten = 0;				// Number of bytes written to the stream.
		
		//wild points buffer used to store the pending wild points to be converted
		HashMap<Long,Double> bufferWildPoints=new HashMap<Long,Double>();
		//wild point occuring times
		long currentTime=0;
		//ArrayList<Long> wildPointsCurrentTime=new ArrayList<Long>();
		//Valid measures array stores the two adjacent valid measures
		String[] validMeasure=new String[2];
		//Variable used for pressure calculations
		double pressure=0.0;
		double previousPressure=0;
		int count; // counter for buffer
		//Flag to distinguish wild point occurence at first
		int wildPointEncounteredFirst=0;

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			try {
				/**
				 * Similar to other filters, we read the 4 bytes of ID.
				 */
				id = 0;
				id_bytes = new byte[4];
				for (i=0; i<IdLength; i++ ){
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if
					//Buffer ID 
					id_bytes[i] = databyte;
					bytesread++;						// Increment the byte count

				} // for

				/****************************************************************************
				// Here we read measurements. All measurement data is read as a stream of bytes
				// and stored as a long value. This permits us to do bitwise manipulation that
				// is neccesary to convert the byte stream into data words. Note that bitwise
				// manipulation is not permitted on any kind of floating point types in Java.
				// If the id = 0 then this is a time value and is therefore a long value - no
				// problem. However, if the id is something other than 0, then the bits in the
				// long value is really of type double and we need to convert the value using
				// Double.longBitsToDouble(long val) to do the conversion which is illustrated.
				// below.
				*****************************************************************************/
				measurement = 0;
				data_bytes = new byte[8];
				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if
					//Buffer the measurements
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count
				} // if
				
				/**
				 * If the id is 0, we have time data to write to the output
				 */
				if ( id == 0 ) {
					//Recording time to use in the data processing later
					currentTime=measurement;
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}
				} // if
				/**
				 * If the id is 3, we have temperature data. 
				 * We can simply convert the double from F to C
				 * Then we fill the measurements byte array with 8 bytes
				 * of the temperature in C so we can write downstream.
				 */
				else if (id == 3) {
					pressure = Double.longBitsToDouble(measurement);
					//Checking if the pressure is a valid point
                    boolean isWild=isWildPoint(previousPressure, pressure, wildPointEncounteredFirst);
                    previousPressure=pressure;
                    if(!isWild){
                    	if(!(bufferWildPoints.size()>0)){
                    		validMeasure[0]=Double.toString(pressure);
                    	}
                    	else{
                    		validMeasure[1]=Double.toString(pressure);
                    	}
                    	//sending the data
    					ByteBuffer.wrap(data_bytes).putDouble(pressure);
    					for (i = 0; i < 4; i++) {
    						WriteFilterOutputPort(id_bytes[i]);
    						byteswritten++;
    					}
    					for (i = 0; i < 8; i++) {
    						WriteFilterOutputPort(data_bytes[i]);
    						byteswritten++;
    					}		
                    }
                    else{
                        if(byteswritten==2)
                            wildPointEncounteredFirst=1;
                    	//sending the wild points first and then try to extrapolate
                        int pressureCode=103;
						ByteBuffer.wrap(pressureType).putDouble(pressureCode);
						for (i = 0; i < 4; i++) {
							WriteFilterOutputPort(pressureType[i]);
							byteswritten++;
						}
                    	ByteBuffer.wrap(data_bytes).putDouble(pressure);
    					for (i = 0; i < 8; i++) {
    						WriteFilterOutputPort(data_bytes[i]);
    						byteswritten++;
    					}	
    					//Store in a buffer to process later and also store corresponding time 
    						bufferWildPoints.put(currentTime, pressure);
    						if(wildPointEncounteredFirst==1 && validMeasure[1]!=null){
    							wildPointEncounteredFirst=0;
    							//Extend wild Points available with last valid measure and send
    							for (Map.Entry<Long, Double> entry : bufferWildPoints.entrySet()) {
    								//replace all values with the first valid point encountered
    								//Sedning pressure type identification. wild or converted (103- wild, 203-converted)
    								
    								int timeCode=001;
    								ByteBuffer.wrap(pressureType).putDouble(timeCode);
    								for (i = 0; i < 4; i++) {
    									WriteFilterOutputPort(pressureType[i]);
    									byteswritten++;
    								}
    								
    								Long timeValue=entry.getKey();
    								ByteBuffer.wrap(data_bytes).putDouble(timeValue);
    		    					for (i = 0; i < 8; i++) {
    		    						WriteFilterOutputPort(data_bytes[i]);
    		    						byteswritten++;
    		    					}
    		    					
    								pressureCode=203;
    								ByteBuffer.wrap(pressureType).putDouble(pressureCode);
    								for (i = 0; i < 4; i++) {
    									WriteFilterOutputPort(pressureType[i]);
    									byteswritten++;
    								}
    								
    				
    		    					double correctedPressureValue=Double.parseDouble(validMeasure[1]);
    								ByteBuffer.wrap(data_bytes).putDouble(correctedPressureValue);
    		    					for (i = 0; i < 8; i++) {
    		    						WriteFilterOutputPort(data_bytes[i]);
    		    						byteswritten++;
    		    					}	
    		    					
    							   // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
    							}
    							validMeasure[0]=validMeasure[1];
    							validMeasure[1]=null;
    							//All the elements corrected so removed
    							bufferWildPoints.clear();
    						}
    						else if(validMeasure[0]!=null && validMeasure[1]!=null){
    							wildPointEncounteredFirst=0;
    							//Extend wild points using first and last valid points
    							for (Map.Entry<Long, Double> entry : bufferWildPoints.entrySet()) {
    								//replace all values with the first valid point encountered
    								//Sedning pressure type identification. wild or converted (103- wild, 203-converted)
    								
    								int timeCode=001;
    								ByteBuffer.wrap(pressureType).putDouble(timeCode);
    								for (i = 0; i < 4; i++) {
    									WriteFilterOutputPort(pressureType[i]);
    									byteswritten++;
    								}
    								
    								Long timeValue=entry.getKey();
    								ByteBuffer.wrap(data_bytes).putDouble(timeValue);
    		    					for (i = 0; i < 8; i++) {
    		    						WriteFilterOutputPort(data_bytes[i]);
    		    						byteswritten++;
    		    					}
    		    					
    								pressureCode=203;
    								ByteBuffer.wrap(pressureType).putDouble(pressureCode);
    								for (i = 0; i < 4; i++) {
    									WriteFilterOutputPort(pressureType[i]);
    									byteswritten++;
    								}
    												
    		    					double correctedPressureValue=((Double.parseDouble(validMeasure[0])+Double.parseDouble(validMeasure[1]))/2);
    								ByteBuffer.wrap(data_bytes).putDouble(correctedPressureValue);
    		    					for (i = 0; i < 8; i++) {
    		    						WriteFilterOutputPort(data_bytes[i]);
    		    						byteswritten++;
    		    					}	
    		    					
    							   // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
    							}
    							validMeasure[0]=validMeasure[1];
    							validMeasure[1]=null;
    							//All the elements corrected so removed
    							bufferWildPoints.clear();
    						}
                    }				
				} // else if
			} // try

			catch (EndOfStreamException e) {
				//if there are some wild points to be corrected, they are corrected here
				//Extend wild points using latest encountered valid point
				for (Map.Entry<Long, Double> entry : bufferWildPoints.entrySet()) {
					//replace all values with the first valid point encountered
					//Sending pressure type identification. wild or converted (103- wild, 203-converted)
					System.out.println("in loop");
					int timeCode=001;
					ByteBuffer.wrap(pressureType).putDouble(timeCode);
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(pressureType[i]);
						byteswritten++;
					}
					
					Long timeValue=entry.getKey();
					data_bytes = new byte[8];
					ByteBuffer.wrap(data_bytes).putDouble(timeValue);
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}
					
					int convertedWildPointCode=203;
					ByteBuffer.wrap(pressureType).putDouble(convertedWildPointCode);
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(pressureType[i]);
						byteswritten++;
					}
					
					double correctedPressureValue=Double.parseDouble(validMeasure[0]);
					ByteBuffer.wrap(data_bytes).putDouble(correctedPressureValue);
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}	
					
				   // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
				}
				validMeasure[0]=validMeasure[1];
				validMeasure[1]=null;
				//All the elements corrected so removed
				bufferWildPoints.clear();
				
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

    } // run
	
	/**
	 * This method OVERRIDES the superclass connect method. It does not take
	 * a generic FilterFramework, rather, it takes a specific Splitter Filter.
	 * While this violates the reusability feature somewhat, it is necessary
	 * since only a Splitter filter in this system has a second output port.
	 * The default Framework does not have multiple outputports.
	 * @param Filter
	 * @param codes
	 * @throws IOException
	 */
	void Connect( SplitterFilterB Filter, HashSet<Integer> codes ) throws IOException {
		Filter.validateConvertedCodes(codes);
		try {
			// Connect this filter's input to the upstream pipe's output stream
			getInputReadPort().connect( Filter.getOutputWritePort3() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect
	
	boolean isWildPoint(double previousPressure, double currentPressure, int wildPointEncounteredFirst){
	if(wildPointEncounteredFirst==1){
		if(currentPressure<0)
			return true;
		else
			return false;
	}
	else{
		if(currentPressure<0)
			return true;
		if(currentPressure>previousPressure && (currentPressure-previousPressure)>10)
			return true;
		if(currentPressure<previousPressure && (previousPressure-currentPressure)>10)
			return true;
		else
			return false;
	}
	}

}
