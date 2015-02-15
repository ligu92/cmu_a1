package SystemA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * This is one of the converter filters for SystemA, it takes temperature
 * data and converts from feet to meters, writing the data to the downstream.
 * 
 * This one is a bit different from the altitude converter. Both this
 * one and the altitude converter have the same upstream. In order to make
 * sure that this filter is hooked up to the correct outputport, we 
 * have to override the Connect method.
 * @author ligu
 *
 */
public class TempConverterA extends FilterFramework {
	/**
	 * Overload the super class constructor
	 * @param codesSet
	 */
	public TempConverterA(HashSet<Integer> codesSet) {
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
		byte[] data_bytes;
		
		//When we read the altitude, we save it
		//into variable so we can convert as specified.
		double temperature = 0.0;
		int byteswritten = 0;				// Number of bytes written to the stream.

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
	void Connect( SplitterFilterA Filter, HashSet<Integer> codes ) throws IOException {
		Filter.validateConvertedCodes(codes);
		try {
			// Connect this filter's input to the upstream pipe's output stream
			getInputReadPort().connect( Filter.getOutputWritePort2() );
			setInputFilter(Filter);

		} // try

		catch( Exception Error )
		{
			System.out.println( "\n" + this.getName() + " FilterFramework error connecting::"+ Error );

		} // catch

	} // Connect

} // TempConverterA