package SystemA;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;

/******************************************************************************************************************
* File:MiddleFilter.java
* Course: 17655
* Project: Assignment 1
* Copyright: Copyright (c) 2003 Carnegie Mellon University
* Versions:
*	1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
*
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
*
* Parameters: 		None
*
* Internal Methods: None
*
******************************************************************************************************************/

public class TempConverterA extends FilterFramework {
	public TempConverterA(HashSet<Integer> codesSet) {
		setInputType(codesSet);
	}

	public void run() {

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		byte[] id_bytes;
		byte[] data_bytes;
		
		//When we read the altitude, we save it
		//into variable so we can convert as specified.
		double temperature = 0.0;
		int byteswritten = 0;				// Number of bytes written to the stream.

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true) {
			/*************************************************************
			*	Here we read a byte and write a byte
			*************************************************************/

			try {
				id = 0;
				id_bytes = new byte[4];
				for (i=0; i<IdLength; i++ ){
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if
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
					data_bytes[i] = databyte;
					bytesread++;									// Increment the byte count
				} // if
				
				if ( id == 0 ) {
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}
				} // if
				else if (id == 4) {
					temperature = Double.longBitsToDouble(measurement);
					temperature = (temperature - 32) / 1.8;
					ByteBuffer.wrap(data_bytes).putDouble(temperature);
					for (i = 0; i < 4; i++) {
						WriteFilterOutputPort(id_bytes[i]);
						byteswritten++;
					}
					for (i = 0; i < 8; i++) {
						WriteFilterOutputPort(data_bytes[i]);
						byteswritten++;
					}			 
				} // else if
			} // try

			catch (EndOfStreamException e) {
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

    } // run
	
	void Connect( SplitterFilterA Filter ) {
		try
		{
			// Connect this filter's input to the upstream pipe's output stream

			getInputReadPort().connect( Filter.getOutputWritePort2() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect

} // MiddleFilter